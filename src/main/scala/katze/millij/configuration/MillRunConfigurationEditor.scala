package katze.millij.configuration

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.ui.components.JBTextField

import javax.swing.JComponent

class MillRunConfigurationEditor extends SettingsEditor[MillRunConfiguration]:
  private val panel = new LabeledComponent[JBTextField]
  
  panel.setComponent(new JBTextField())
  panel.setText("Task name")

  override def resetEditorFrom(s: MillRunConfiguration): Unit = 
    panel.getComponent.setText(s.taskName)
  end resetEditorFrom

  override def applyEditorTo(s: MillRunConfiguration): Unit = 
    s.taskName = panel.getComponent.getText
  end applyEditorTo

  override def createEditor(): JComponent = panel
end MillRunConfigurationEditor

