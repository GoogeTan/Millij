package katze.millij.wizard

import com.intellij.ui.dsl.builder.Panel

object JavaMillLanguageConfigurator extends MillLanguageConfigurator:
  override def setupUI(builder: Panel): Unit =
    ()
  end setupUI

  override def generateConfig(isYaml: Boolean): String =
    if isYaml then
      s"""
         |extends: JavaModule
         |""".stripMargin
    else
      s"""import mill._
         |object root extends JavaModule
         |""".stripMargin
  end generateConfig
end JavaMillLanguageConfigurator
