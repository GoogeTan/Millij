package katze.millij.annotator

import com.intellij.lang.annotation.HighlightSeverity
import katze.millij.isObjectDeclarationText
import katze.millij.psi.PsiChild
import org.jetbrains.yaml.psi.{YAMLDocument, YAMLKeyValue, YAMLMapping}

import scala.annotation.tailrec

def objectInInappropriatePlace : CoolAnnotator[YAMLKeyValue, YAMLMapping] =
  case (kv, mapping, annotationHolder) =>
    @tailrec
    def isPathCorrect(kv : YAMLKeyValue) : Boolean = kv match
      case PsiChild(self : YAMLKeyValue, parent : YAMLMapping, further : YAMLDocument, _*) =>
        true
      case PsiChild(self : YAMLKeyValue, parent : YAMLMapping, further : YAMLKeyValue, _*) if isObjectDeclarationText(further.getKeyText) =>
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

