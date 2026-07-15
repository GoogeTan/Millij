package katze.millij.annotator

import com.intellij.lang.annotation.{AnnotationHolder, Annotator}
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import katze.millij.cool.CoolPattern

/**
 * Adapts [[CoolAnnotator]] to [[Annotator]]
 */
open class CoolAnnotatorAdapter[
  Data,
](
  cool : CoolAnnotator[Data],
  coolPattern: CoolPattern[Data],
) extends Annotator:
  final override def annotate(psiElement: PsiElement, annotationHolder: AnnotationHolder): Unit =
    coolPattern.extract(psiElement).foreach(cool(_, annotationHolder))
  end annotate
end CoolAnnotatorAdapter

/**
 * Adapts [[CoolAnnotator]] to [[Annotator]]
 */
final class DumbCoolAnnotatorAdapter[
  Data,
](
  cool : CoolAnnotator[Data],
  coolPattern: CoolPattern[Data],
) extends CoolAnnotatorAdapter(cool, coolPattern) with DumbAware


