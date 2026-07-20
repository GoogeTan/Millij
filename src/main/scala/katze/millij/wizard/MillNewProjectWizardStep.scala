package katze.millij.wizard

import com.intellij.ide.wizard.{AbstractNewProjectWizardStep, NewProjectWizardStep}
import com.intellij.openapi.progress.{ProgressIndicator, ProgressManager, Task}
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Panel
import katze.millij.externalSystem.MillRunner
import org.jetbrains.sbt.project.template.wizard.ScalaNewProjectWizardMultiStep
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.SystemInfo
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.util.io.HttpRequests
import katze.millij.wizard.MillNewProjectWizardStep.Log

import java.nio.file.{Files, Path}
import javax.swing.{ButtonGroup, JRadioButton}

trait MillLanguageConfigurator:
  def setupUI(builder: Panel): Unit
  def generateConfig(isYaml: Boolean): String
end MillLanguageConfigurator

final class MillNewProjectWizardStep(
  parent: NewProjectWizardStep,
  language: MillLanguageConfigurator
) extends AbstractNewProjectWizardStep(parent):
  private val millVersionField = new JBTextField("1.1.5")
  private val buildScRadio = new JRadioButton("build.sc", true)
  private val buildMillRadio = new JRadioButton("build.mill")
  private val buildYamlRadio = new JRadioButton("build.yaml")
  private val radioGroup = new ButtonGroup()
  radioGroup.add(buildScRadio)
  radioGroup.add(buildMillRadio)
  radioGroup.add(buildYamlRadio)

  override def setupUI(builder: Panel): Unit =
    super.setupUI(builder)

    builder.row("Mill Version:",
      row =>
        row.cell(millVersionField)
        kotlin.Unit.INSTANCE
    )

    language.setupUI(builder)

    builder.buttonsGroup("Configuration File:", false,
      panel =>
        panel.row(
          "",
          row =>
            row.cell(buildScRadio)
            row.cell(buildMillRadio)
            row.cell(buildYamlRadio)
            kotlin.Unit.INSTANCE
        )
        kotlin.Unit.INSTANCE
    )
  end setupUI

  override def setupProject(project: Project): Unit =
    super.setupProject(project)

    val projectDir = getContext.getProjectDirectory

    val configFilename = if buildScRadio.isSelected then
      "build.sc"
    else if buildMillRadio.isSelected then
      "build.mill"
    else
      "build.mill.yaml"
    generateMillBuildFile(projectDir, configFilename)
    val millVersion = millVersionField.getText
    ProgressManager.getInstance().run(
      new Task.Backgroundable(project, "Setting up Mill project...", false):
        override def run(indicator: ProgressIndicator): Unit =
          if downloadMill(projectDir, indicator, millVersion) then
            val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(projectDir)
            if virtualFile != null then
              MillRunner.installAndRefreshBsp(project, virtualFile)
            else
              Log.error("Project directory not found in Virtual File System after downloading Mill")
          else
            Log.error("Failed to download Mill scripts")
    )
  end setupProject

  private def generateMillBuildFile(dir: Path, configFilename: String): Unit =
    val millVersion = millVersionField.getText
    val isYaml = buildYamlRadio.isSelected
    val configText = language.generateConfig(isYaml)
    Files.writeString(dir.resolve(configFilename), configText)
    Files.writeString(dir.resolve(".mill-version"), millVersion)
  end generateMillBuildFile

  def downloadMill(targetDir: Path, indicator: ProgressIndicator, millVersion : String): Boolean =
    val baseUrl = s"https://repo1.maven.org/maven2/com/lihaoyi/mill-dist/$millVersion"
    
    val shRemote = s"mill-dist-$millVersion-mill.sh"
    val batRemote = s"mill-dist-$millVersion-mill.bat"
    
    val shLocal = targetDir.resolve("mill").toFile
    val batLocal = targetDir.resolve("mill.bat").toFile

    try
      indicator.setText(s"Downloading Mill $millVersion scripts...")
      indicator.setIndeterminate(false)

      indicator.setText2("Downloading mill script...")
      HttpRequests.request(s"$baseUrl/$shRemote")
        .productNameAsUserAgent()
        .saveToFile(shLocal, indicator)
      shLocal.setExecutable(true, false)

      indicator.setText2("Downloading mill.bat script...")
      HttpRequests.request(s"$baseUrl/$batRemote")
        .productNameAsUserAgent()
        .saveToFile(batLocal, indicator)

      true
    catch
      case e: Exception =>
        Log.error(s"Failed to download Mill scripts", e)
        false
  end downloadMill
end MillNewProjectWizardStep

object MillNewProjectWizardStep:
  private val Log = Logger.getInstance(classOf[MillNewProjectWizardStep])
