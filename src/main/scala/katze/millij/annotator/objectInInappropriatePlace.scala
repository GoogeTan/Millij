package katze.millij.annotator

import com.intellij.lang.annotation.HighlightSeverity
import katze.millij.place.isObjectDeclarationText
import katze.millij.psi.PsiChild
import org.jetbrains.yaml.psi.{YAMLDocument, YAMLKeyValue, YAMLMapping}

import scala.annotation.tailrec

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
        .tooltip("Module declaration is not allowed inside of object instantiation")
        .create()
    end if
end objectInInappropriatePlace

