package katze.millij

import _root_.kotlin.coroutines.Continuation
import com.intellij.jarRepository.{RemoteRepositoriesConfiguration, RemoteRepositoryDescription}
import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.{ProjectActivity, StartupActivity}
import katze.millij.externalSystem.MillBspProjectAware

import java.nio.file.{Files, Paths}
import scala.jdk.CollectionConverters.*

final class MillProjectStartupActivity extends StartupActivity.DumbAware:
  override def runActivity(project: Project): Unit =
    val tracker = ExternalSystemProjectTracker.getInstance(project)
    val aware = MillBspProjectAware(project)
    tracker.register(aware)
  end runActivity
end MillProjectStartupActivity