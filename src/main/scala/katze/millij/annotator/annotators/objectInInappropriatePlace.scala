package katze.millij.annotator.annotators

import com.intellij.lang.annotation.HighlightSeverity
import katze.millij.annotator.lib.CoolAnnotator
import katze.millij.data.MillijBundle
import katze.millij.place.isObjectDeclarationText
import katze.millij.psi.PsiChild
import org.jetbrains.yaml.psi.{YAMLDocument, YAMLKeyValue, YAMLMapping}

import scala.annotation.tailrec

/**
 * Annotates objects declaration with error if it is defined in inappropriate place.
 */
def objectInInappropriatePlace : CoolAnnotator[(YAMLKeyValue, YAMLMapping)] =
  case ((kv, _), annotationHolder) =>
    @tailrec
    def isPathCorrect(kv : YAMLKeyValue) : Boolean = kv match
      case PsiChild(_ : YAMLKeyValue, _ : YAMLMapping, _ : YAMLDocument, _*) =>
        true
      case PsiChild(_ : YAMLKeyValue, _ : YAMLMapping, further : YAMLKeyValue, _*) if isObjectDeclarationText(further.getKeyText) =>
        isPathCorrect(further)
      case _ =>
        false

    if isObjectDeclarationText(kv.getKeyText) && !isPathCorrect(kv) then
      annotationHolder
        .newSilentAnnotation(HighlightSeverity.ERROR)
        .range(kv)
        .tooltip(MillijBundle.message("module.declaration.inappropriate.place.error"))
        .create()
    end if
end objectInInappropriatePlace