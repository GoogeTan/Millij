package katze.millij.annotator.lib

import com.intellij.lang.annotation.{AnnotationHolder, Annotator}
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import katze.millij.cool.CoolPattern
import katze.millij.data.Smart

/**
 * Adapts [[CoolAnnotator]] to [[Annotator]]
 */
def SmartCoolAnnotatorAdapter[
  Data,
](
  cool : Smart ?=> CoolAnnotator[Data],
  coolPattern: CoolPattern[Data],
)(psiElement: PsiElement, annotationHolder: AnnotationHolder)(using Smart): Unit =
    coolPattern.extract(psiElement).foreach(cool(_, annotationHolder))
end SmartCoolAnnotatorAdapter