package katze.millij.externalSystem

import com.intellij.openapi.Disposable
import com.intellij.openapi.externalSystem.autoimport.*
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.project.{Project, ProjectUtil}

import scala.jdk.CollectionConverters.*

final class MillBspProjectAware(project: Project) extends ExternalSystemProjectAware:
  // Track the .mill.yaml file for changes
  override def getSettingsFiles: java.util.Set[String] =
    Option(ProjectUtil.guessProjectDir(project))
      .flatMap { dir =>
        try {
          Some(dir.toNioPath.resolve(".mill.yaml").toString)
        } catch {
          case _: UnsupportedOperationException =>
            Some(s"${dir.getPath}/.mill.yaml")
        }
      }
      .map(Set(_))
      .getOrElse(Set.empty)
      .asJava

  override def subscribe(
    listener: ExternalSystemProjectListener,
    parentDisposable: Disposable
  ): Unit = () // Can remain empty for simple file-watch polling

  override def reloadProject(context: ExternalSystemProjectReloadContext): Unit =
    // When the user clicks the reload icon, execute the shared Mill runner
    Option(ProjectUtil.guessProjectDir(project)).foreach { baseDir =>
      MillRunner.installAndRefreshBsp(project, baseDir)
    }
  end reloadProject

  override def getProjectId: ExternalSystemProjectId =
    ExternalSystemProjectId(ProjectSystemId("BSP"), project.getName)
end MillBspProjectAware