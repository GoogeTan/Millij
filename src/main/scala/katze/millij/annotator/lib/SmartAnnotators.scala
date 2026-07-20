package katze.millij.annotator.lib

import com.intellij.lang.annotation.{AnnotationHolder, Annotator}
import com.intellij.psi.PsiElement
import katze.millij.data.Smart

/**
 * Smart annotator that runs smart annotators one by one
 */
open class SmartAnnotators(list : List[Smart ?=> (PsiElement, AnnotationHolder) => Unit]) extends Annotator:
  override def annotate(psiElement: PsiElement, annotationHolder: AnnotationHolder): Unit =
    Smart(psiElement.getProject) {
      list.foreach(_(psiElement, annotationHolder))
    }
  end annotate
end SmartAnnotators