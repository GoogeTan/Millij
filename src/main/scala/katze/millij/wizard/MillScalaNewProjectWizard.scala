package katze.millij.wizard

import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ui.components.{JBLabel, JBTextField}
import org.jetbrains.sbt.project.template.wizard.ScalaNewProjectWizardMultiStep
import org.jetbrains.sbt.project.template.wizard.buildSystem.BuildSystemScalaNewProjectWizard

final class MillScalaNewProjectWizard extends BuildSystemScalaNewProjectWizard:
  override def getName: String = "Mill"

  override def createStep(parent: ScalaNewProjectWizardMultiStep): NewProjectWizardStep =
    new MillNewProjectWizardStep(
      parent,
      ScalaMillLanguageConfigurator()
    )
  end createStep
end MillScalaNewProjectWizard
