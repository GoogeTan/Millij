package katze.millij.wizard

import com.intellij.ui.components.{JBLabel, JBTextField}
import com.intellij.ui.dsl.builder.Panel

class ScalaMillLanguageConfigurator extends MillLanguageConfigurator:
  val scalaVersion = JBTextField("3.3.7")

  override def setupUI(builder: Panel): Unit =
    builder.row(
      JBLabel("Scala version"),
      row =>
        row.cell(scalaVersion)
        kotlin.Unit.INSTANCE
    )
  end setupUI

  override def generateConfig(isYaml: Boolean): String =
    if isYaml then
      s"""
         |extends: ScalaModule
         |scalaVersion: ${scalaVersion.getText}
         |""".stripMargin
    else
      s"""import mill._
         |import scalalib._
         |
         |object root extends ScalaModule {
         |  def scalaVersion = "${scalaVersion.getText}"
         |}
         |""".stripMargin
    end if
  end generateConfig
end ScalaMillLanguageConfigurator
