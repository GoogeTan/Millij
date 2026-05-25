package katze.millij.wizard

import com.intellij.ide.projectWizard.generators.{BuildSystemJavaNewProjectWizard, JavaNewProjectWizard}
import com.intellij.ide.wizard.NewProjectWizardStep
import com.intellij.ui.components.{JBLabel, JBTextField}
import com.intellij.ui.dsl.builder.Panel

class MillJavaNewProjectWizard extends BuildSystemJavaNewProjectWizard:
  override def getName: String = "Mill"

  override def createStep(parent: JavaNewProjectWizard.Step): NewProjectWizardStep =
    new MillNewProjectWizardStep(
      parent,
      JavaMillLanguageConfigurator
    )
  end createStep
end MillJavaNewProjectWizard
