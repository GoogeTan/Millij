package katze.millij.configuration

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.{OSProcessHandler, ProcessHandler}
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project
import org.jdom.Element

class MillRunConfiguration(project: Project, factory: ConfigurationFactory, name: String)
  extends LocatableConfigurationBase[RunProfileState](project, factory, name):

  var taskName: String = ""

  override def getConfigurationEditor: SettingsEditor[_ <: RunConfiguration] = new MillRunConfigurationEditor()

  override def getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    new CommandLineState(environment):
      override def startProcess(): ProcessHandler =
        val commandLine = new GeneralCommandLine("mill", "--no-server", taskName)
        commandLine.setWorkDirectory(project.getBasePath)
        new OSProcessHandler(commandLine)
      end startProcess
    end new
  end getState

  override def readExternal(element: Element): Unit =
    super.readExternal(element)
    taskName = element.getChildText("taskName")
  end readExternal

  override def writeExternal(element: Element): Unit =
    super.writeExternal(element)
    val taskNameElement = new Element("taskName")
    taskNameElement.setText(taskName)
    element.addContent(taskNameElement)
  end writeExternal
end MillRunConfiguration
