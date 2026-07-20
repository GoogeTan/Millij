package katze.millij.externalSystem

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.externalSystem.autoimport.*
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.project.{Project, ProjectUtil}
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.{VfsUtilCore, VirtualFile, VirtualFileVisitor}
import com.intellij.psi.search.{FilenameIndex, GlobalSearchScope}

import java.util
import scala.jdk.CollectionConverters.*

final class MillBspProjectAware(project: Project) extends AbstractMillBspProjectAware:

  private val listeners = new java.util.concurrent.CopyOnWriteArrayList[ExternalSystemProjectListener]()

  val millConfigNames =
    Set(
      "build.mill.yaml",
      "build.sc",
      "build.mill",
      "package.mill.yaml",
      "package.mill",//TODO do I need package.sc?
    )
  
  override def getSettingsFiles: java.util.Set[String] =
    ReadAction.computeBlocking: () =>
      val files = new java.util.HashSet[String]()
      val scope = GlobalSearchScope.projectScope(project)

      millConfigNames.foreach { configName =>
        FilenameIndex.getVirtualFilesByName(configName, scope).forEach { file =>
          files.add(file.getPath)
        }
      }

      files
  end getSettingsFiles

  override def subscribe(
    listener: ExternalSystemProjectListener,
    parentDisposable: Disposable
  ): Unit =
    listeners.add(listener)
    Disposer.register(parentDisposable, () => listeners.remove(listener))
  end subscribe

  override def reloadProject(context: ExternalSystemProjectReloadContext): Unit =
    Option(ProjectUtil.guessProjectDir(project)).foreach { baseDir =>
      listeners.forEach(_.onProjectReloadStart())
      MillRunner.installAndRefreshBsp(project, baseDir, Some { success =>
        val status = if success then ExternalSystemRefreshStatus.SUCCESS else ExternalSystemRefreshStatus.FAILURE
        listeners.forEach(_.onProjectReloadFinish(status))
      })
    }
  end reloadProject

  override def getProjectId: ExternalSystemProjectId =
    val basePath = Option(project.getBasePath).getOrElse("")
    ExternalSystemProjectId(ProjectSystemId("BSP"), basePath)
  end getProjectId
end MillBspProjectAware