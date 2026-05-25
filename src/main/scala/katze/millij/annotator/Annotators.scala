package katze.millij.annotator

import com.intellij.lang.annotation.{AnnotationHolder, Annotator}
import com.intellij.psi.PsiElement

/**
 * Annotator that runs annotators one by one
 */
open class Annotators(list : List[Annotator]) extends Annotator:
  final override def annotate(psiElement: PsiElement, annotationHolder: AnnotationHolder): Unit =
    list.foreach(_.annotate(psiElement, annotationHolder))
  end annotate
end Annotators
