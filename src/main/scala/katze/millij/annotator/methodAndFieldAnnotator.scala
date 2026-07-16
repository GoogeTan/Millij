package katze.millij.annotator

import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.lang.annotation.{AnnotationHolder, HighlightSeverity}
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import katze.millij.*
import katze.millij.data.Smart
import katze.millij.place.*
import katze.millij.psi.*
import org.jetbrains.yaml.psi.{YAMLKeyValue, YAMLMapping}

/**
 * Marks element as error if it is a not a correct member of scope.
 */
def unexistingMembersAnnotator(
  checkIfItIsACorrectMember : (YAMLMapping, YAMLKeyValue) => Option[String]
) : CoolAnnotator[(YAMLKey[PsiElement], YAMLKeyValue, YAMLMapping)] =
  case ((element, keyValue, mapping), annotationHolder) =>
    if isMemberName(keyValue.getKeyText) then
      checkIfItIsACorrectMember(mapping, keyValue).foreach(
        error =>
          annotationHolder
            .newSilentAnnotation(
              HighlightSeverity.ERROR,
            )
            .tooltip(error)
            .highlightType(ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
            .range(element)
            .create()
      )
    end if
end unexistingMembersAnnotator

/**
 * Annotates keys of module declaration, object parameter declaration, module member declaration. 
 */
def methodAndFieldAnnotator(scopeOf : YAMLKeyValueWithNotKey["extends"] => Option[Boolean]) : CoolAnnotator[(YAMLKeyValueWithNotKey["extends"], YAMLMapping)] =
  case ((kv, mapping), annotationHolder) =>
    scopeOf(kv) match
      case Some(true) =>
        if isObjectDeclarationText(kv.getKeyText) then
          annotateObjectKeyValue(kv, annotationHolder)
        else
          annotateModuleMemeber(kv, annotationHolder)
        end if
      case Some(false) =>
        annotationHolder
          .newSilentAnnotation(HighlightSeverity.INFORMATION)
          .range(kv.getKey)
          .textAttributes(MillijTextStyles.MILL_YAML_NAMED_ARGUMENT)
          .create()
      case None =>
end methodAndFieldAnnotator

def annotateObjectKeyValue(keyValue: YAMLKeyValue, annotationHolder: AnnotationHolder) : Unit =
  val (objectKeywordRange, nameRange) = objectTextRanges(keyValue.getKey.getTextRange)

  annotationHolder
    .newSilentAnnotation(HighlightSeverity.INFORMATION)
    .textAttributes(MillijTextStyles.OBJECT_KEYWORD)
    .range(objectKeywordRange)
    .create()

  annotationHolder
    .newSilentAnnotation(HighlightSeverity.INFORMATION)
    .textAttributes(MillijTextStyles.MILL_YAML_MODULE_NAME)
    .range(nameRange)
    .create()
end annotateObjectKeyValue

def annotateModuleMemeber(keyValue: YAMLKeyValue, annotationHolder: AnnotationHolder) : Unit =
  annotationHolder
    .newSilentAnnotation(HighlightSeverity.INFORMATION)
    .range(keyValue.getKey)
    .textAttributes(MillijTextStyles.MILL_YAML_MODULE_MEMBER)
    .create()
end annotateModuleMemeber
  

//TODO rename me
/**
 * Calculates text ranges that indicate object keyword and module name in module declaration
 * 
 * Note that it assumes that text range is actually a module declaration so results for any other text is meaningless.
 */
def objectTextRanges(textRange : TextRange) : (TextRange, TextRange) =
  val objectText = "object"
  val objectKeywordRange = TextRange.create(textRange.getStartOffset, textRange.getStartOffset + objectText.length)
  val nameRange = TextRange.create(textRange.getStartOffset + objectText.length, textRange.getEndOffset)
  (objectKeywordRange, nameRange)
end objectTextRanges