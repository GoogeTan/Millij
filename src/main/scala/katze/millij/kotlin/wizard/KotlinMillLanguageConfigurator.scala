package katze.millij.kotlin.wizard

import com.intellij.ui.components.{JBLabel, JBTextField}
import com.intellij.ui.dsl.builder.Panel
import katze.millij.wizard.MillLanguageConfigurator

class KotlinMillLanguageConfigurator extends MillLanguageConfigurator:
  val kotlinVersionTextField = JBTextField("3.3.7")

  override def setupUI(builder: Panel): Unit =
    builder.row(
      JBLabel("Kotlin version"),
      row =>
        row.cell(kotlinVersionTextField)
        kotlin.Unit.INSTANCE
    )
  end setupUI

  override def generateConfig(isYaml: Boolean): String =
    if isYaml then
      s"""# build.yaml
         |extends: KotlinModule
         |kotlinVersion: ${kotlinVersionTextField.getText}
         |""".stripMargin
    else
      s"""import mill._
         |import kotlinlib._
         |
         |object root extends KotlinModule {
         |  def kotlinVersion = "${kotlinVersionTextField.getText}"
         |}
         |""".stripMargin
    end if
  end generateConfig
end KotlinMillLanguageConfigurator
