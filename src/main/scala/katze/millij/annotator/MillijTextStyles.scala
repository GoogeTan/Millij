package katze.millij.annotator

import com.intellij.openapi.editor.DefaultLanguageHighlighterColors
import com.intellij.openapi.editor.colors.TextAttributesKey

object MillijTextStyles:
  val OBJECT_KEYWORD: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
    "MILL_YAML_OBJECT_KEYWORD",
    DefaultLanguageHighlighterColors.KEYWORD
  )

  val MILL_YAML_MODULE_NAME: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
    "MILL_YAML_MODULE_NAME",
    DefaultLanguageHighlighterColors.CLASS_NAME
  )

  val MILL_YAML_MODULE_MEMBER: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
    "MILL_YAML_MODULE_MEMBER",
    DefaultLanguageHighlighterColors.FUNCTION_DECLARATION
  )

  val MILL_YAML_NAMED_ARGUMENT = TextAttributesKey.createTextAttributesKey(
    "MILL_YAML_NAMED_ARGUMENT",
    TextAttributesKey.find(
      "Scala Named Argument"
    )
  )

  val MILL_MAVEN_DEPENDENCY: TextAttributesKey = TextAttributesKey.createTextAttributesKey(
    "MILL_MAVEN_DEPENDENCY",
    DefaultLanguageHighlighterColors.REASSIGNED_PARAMETER
  )
end MillijTextStyles
