package katze.millij.annotator

import com.intellij.lang.annotation.{AnnotationHolder, Annotator}
import com.intellij.psi.PsiElement
import katze.millij.cool.CoolPattern

/**
 * Adapts [[CoolAnnotator]] to [[Annotator]]
 */
final class CoolAnnotatorAdapter[
  Data,
](
  cool : CoolAnnotator[Data],
  coolPattern: CoolPattern[Data],
) extends Annotator:
  override def annotate(psiElement: PsiElement, annotationHolder: AnnotationHolder): Unit =
    coolPattern.extract(psiElement).foreach(cool(_, annotationHolder))
  end annotate
end CoolAnnotatorAdapter


