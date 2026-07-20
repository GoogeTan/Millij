package katze.millij.annotator.lib

import com.intellij.lang.annotation.{AnnotationHolder, Annotator}
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import katze.millij.cool.CoolPattern

/**
 * Adapts [[CoolAnnotator]] to [[Annotator]]
 */
final class DumbCoolAnnotatorAdapter[
  Data,
](
  cool : CoolAnnotator[Data],
  coolPattern: CoolPattern[Data],
) extends Annotator with DumbAware:
  final override def annotate(psiElement: PsiElement, annotationHolder: AnnotationHolder): Unit =
    coolPattern.extract(psiElement).foreach(cool(_, annotationHolder))
  end annotate
end DumbCoolAnnotatorAdapter