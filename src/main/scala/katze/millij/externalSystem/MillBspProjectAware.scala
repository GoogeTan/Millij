package katze.millij.externalSystem

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.externalSystem.autoimport.*
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.project.{Project, ProjectUtil}
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.{VfsUtilCore, VirtualFile, VirtualFileVisitor}

import java.util
import scala.jdk.CollectionConverters.*

final class MillBspProjectAware(project: Project) extends ExternalSystemProjectAware:

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
      val files = new util.HashSet[String]()
      Option(ProjectUtil.guessProjectDir(project)).foreach { baseDir =>
        VfsUtilCore.visitChildrenRecursively(baseDir,
          new VirtualFileVisitor[Void]() {
            override def visitFile(file: VirtualFile): Boolean =
              if !file.isDirectory && millConfigNames.contains(file.getName) then
                files.add(file.getPath)
              true
          }
        )
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