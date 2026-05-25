package katze.millij.kotlin.wizard

import com.intellij.ide.wizard.NewProjectWizardStep
import katze.millij.wizard.MillNewProjectWizardStep
import org.jetbrains.kotlin.tools.projectWizard.{BuildSystemKotlinNewProjectWizard, KotlinNewProjectWizard}

class MillKotlinNewProjectWizard extends BuildSystemKotlinNewProjectWizard:
  override def getName: String = "Mill"

  override def createStep(p: KotlinNewProjectWizard.Step): NewProjectWizardStep =
    new MillNewProjectWizardStep(
      p,
      KotlinMillLanguageConfigurator()
    )
  end createStep
end MillKotlinNewProjectWizard
