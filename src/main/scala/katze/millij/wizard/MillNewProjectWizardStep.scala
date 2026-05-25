package katze.millij.wizard

import com.intellij.ide.wizard.{AbstractNewProjectWizardStep, NewProjectWizardStep}
import com.intellij.openapi.project.Project
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.components.JBTextField
import org.jetbrains.sbt.project.template.wizard.ScalaNewProjectWizardMultiStep

import java.nio.file.Files
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
      "build.yaml"
    generateMillBuildFile(projectDir, configFilename)
  end setupProject

  private def generateMillBuildFile(dir: java.nio.file.Path, configFilename: String): Unit =
    val millVersion = millVersionField.getText
    val isYaml = buildYamlRadio.isSelected
    val configText = language.generateConfig(isYaml)
    Files.writeString(dir.resolve(configFilename), configText)
    Files.writeString(dir.resolve(".mill-version"), millVersion)
  end generateMillBuildFile
end MillNewProjectWizardStep
