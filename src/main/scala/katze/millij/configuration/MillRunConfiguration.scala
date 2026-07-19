package katze.millij.configuration

import com.intellij.execution.Executor
import com.intellij.execution.configurations.*
import com.intellij.execution.process.{OSProcessHandler, ProcessHandler}
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.{Project, ProjectUtil}
import katze.millij.externalSystem.MillRunner
import org.jdom.Element

class MillRunConfiguration(project: Project, factory: ConfigurationFactory, name: String)
  extends LocatableConfigurationBase[RunProfileState](project, factory, name):

  var taskName: String = ""

  override def getConfigurationEditor: SettingsEditor[? <: RunConfiguration] = new MillRunConfigurationEditor()

  override def getState(executor: Executor, environment: ExecutionEnvironment): RunProfileState =
    new CommandLineState(environment):
      override def startProcess(): ProcessHandler = 
        val projectDir = ProjectUtil.guessProjectDir(project)
        if projectDir == null then
          throw RuntimeConfigurationError("Couldn't determine project directory")
        end if
        val millExecutable = MillRunner.findMillExecutable(projectDir).getOrElse(throw RuntimeConfigurationError("Mill executable or script was not found. Please add a script or install mill globally"))//TODO add suggestion to download a script for user
        val commandLine = new GeneralCommandLine(millExecutable, "--no-server", taskName)
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
