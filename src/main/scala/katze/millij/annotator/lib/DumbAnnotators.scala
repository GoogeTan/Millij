package katze.millij.annotator.lib

import com.intellij.lang.annotation.{AnnotationHolder, Annotator}
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement

/**
 * Dumb annotator that runs dumb annotators one by one
 */
open class DumbAnnotators(list : List[Annotator & DumbAware]) extends Annotator with DumbAware:
  final override def annotate(psiElement: PsiElement, annotationHolder: AnnotationHolder): Unit =
    list.foreach(_.annotate(psiElement, annotationHolder))
  end annotate
end DumbAnnotators