package katze.millij.annotator

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

object MillTextStyles:
  val OBJECT_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
    "MY_YAML_OBJECT_KEYWORD",
    DefaultLanguageHighlighterColors.KEYWORD
  )

  val OBJECT_NAME: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
    "MY_YAML_OBJECT_NAME",
    DefaultLanguageHighlighterColors.CLASS_NAME
  )

  val FUNCTION_DECLARATION: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
    "MY_YAML_FUNCTION_DECLARATION",
    DefaultLanguageHighlighterColors.FUNCTION_DECLARATION
  )

  val OVERRIDE_RHS = TextAttributesKey.createTextAttributesKey(
    "MY_YAML_OVERRIDE_RHS",
    TextAttributesKey.find(
      "Scala Named Argument"
    )
  )

  val MAVEN_DEPENDENCY: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
    "MY_YAML_MAVEN_DEPENDENCY",
    DefaultLanguageHighlighterColors.REASSIGNED_PARAMETER
  )
end MillTextStyles
