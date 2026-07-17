package katze.millij

import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectTracker
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity
import katze.millij.externalSystem.MillBspProjectAware

final class MillProjectStartupActivity extends StartupActivity.DumbAware:
  override def runActivity(project: Project): Unit =
    val tracker = ExternalSystemProjectTracker.getInstance(project)
    val aware = MillBspProjectAware(project)
    tracker.register(aware)
  end runActivity
end MillProjectStartupActivity