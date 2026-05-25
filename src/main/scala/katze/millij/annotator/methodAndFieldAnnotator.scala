package katze.millij.annotator

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import katze.millij.*
import katze.millij.place.*
import katze.millij.psi.*
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.yaml.psi.{YAMLKeyValue, YAMLMapping}

def unexistingMembersAnnotator(
  checkIfItIsACorrectMember : (YAMLMapping, YAMLKeyValue) => Option[String]
) : CoolAnnotator[YAMLKey[PsiElement], (YAMLKeyValue, YAMLMapping)] =
  case (element, (keyValue, mapping), annotationHolder) =>
    if !isObjectDeclarationText(keyValue.getKeyText) && !isExtendsBlock(keyValue.getKeyText) then
      checkIfItIsACorrectMember(mapping, keyValue).foreach(
        error =>
          annotationHolder
            .newSilentAnnotation(
              HighlightSeverity.ERROR,//TODO make references work
            )
            .tooltip(error)
            .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
            .range(element)
            .create()
      )
    end if
end unexistingMembersAnnotator

def scopeNameForUnexistingMembersAnnotator : PlaceInYamlConfig[ScType] => String =
  case PlaceInYamlConfig.Module(extendList, definedMembers) =>
    s"module which extends ${extendList.mkString(", ")}"
  case PlaceInYamlConfig.Member(parentTypes, name, expectedType, definedMembers) =>
    s"object of type <a href=${expectedType}>${expectedType}</a>"
end scopeNameForUnexistingMembersAnnotator

def methodAndFieldAnnotator : CoolAnnotator[YAMLKeyValueWithNotKey["extends"], YAMLMapping] =
  case (kv, mapping, annotationHolder) =>
    richScopeOf(kv).foreach:
      case PlaceInYamlConfig.Module(extendList, _) =>
        if isObjectDeclarationText(kv.getKeyText) then
          val (objectKeywordRange, nameRange) = objectTextRanges(kv.getKey.getTextRange)

          annotationHolder
            .newSilentAnnotation(HighlightSeverity.INFORMATION)
            .textAttributes(MillTextStyles.OBJECT_KEYWORD)
            .range(objectKeywordRange)
            .create()

          annotationHolder
            .newSilentAnnotation(HighlightSeverity.INFORMATION)
            .textAttributes(MillTextStyles.OBJECT_NAME)
            .range(nameRange)
            .create()

        else if !isExtendsBlock(kv.getKeyText) then
          annotationHolder
            .newSilentAnnotation(HighlightSeverity.INFORMATION)
            .range(kv.getKey)
            .textAttributes(MillTextStyles.FUNCTION_DECLARATION)
            .create()
        end if

      case PlaceInYamlConfig.Member(parentTypes, name, expectedType, _) =>
        annotationHolder
          .newSilentAnnotation(HighlightSeverity.INFORMATION)
          .range(kv.getKey)
          .textAttributes(MillTextStyles.OVERRIDE_RHS)
          .create()
end methodAndFieldAnnotator

