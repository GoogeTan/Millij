package katze.millij.annotator

import com.intellij.lang.annotation.{AnnotationHolder, Annotator}
import com.intellij.openapi.project.DumbAware
import com.intellij.psi.PsiElement
import katze.millij.data.Smart

/**
 * Annotator that runs annotators one by one
 */
open class Annotators(list : List[Annotator]) extends Annotator:
  final override def annotate(psiElement: PsiElement, annotationHolder: AnnotationHolder): Unit =
    list.foreach(_.annotate(psiElement, annotationHolder))
  end annotate
end Annotators

/**
 * Smart annotator that runs smart annotators one by one
 */
open class SmartAnnotators(list : Smart ?=> List[Annotator]) extends Annotator:
  override def annotate(psiElement: PsiElement, annotationHolder: AnnotationHolder): Unit =
    Smart(psiElement.getProject) {
      list.foreach(_.annotate(psiElement, annotationHolder))
    }
  end annotate
end SmartAnnotators

/**
 * Dumb annotator that runs dumb annotators one by one
 */
open class DumbAnnotators(list : List[Annotator & DumbAware]) extends Annotators(list) with DumbAware

