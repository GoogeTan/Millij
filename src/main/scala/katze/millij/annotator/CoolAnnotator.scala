package katze.millij.annotator

import com.intellij.lang.annotation.{AnnotationHolder, Annotator}
import com.intellij.psi.PsiElement
import katze.millij.cool.{PsiElementMatcher, PsiParent}

type CoolAnnotator[Element, Parents] = (Element, Parents, AnnotationHolder) => Unit

final class CoolAnnotatorAdapter[
  Element <: PsiElement : PsiElementMatcher as EPEM,
  Parents : PsiParent as PPP
](cool : CoolAnnotator[Element, Parents]) extends Annotator:
  override def annotate(psiElement: PsiElement, annotationHolder: AnnotationHolder): Unit =
    (EPEM.extract(psiElement), PPP.test(psiElement.getParent)) match
      case (Some(element), Some(parent)) =>
        cool(element, parent, annotationHolder)
      case _ =>
    end match
  end annotate
end CoolAnnotatorAdapter

open class Annotators(list : List[Annotator]) extends Annotator:
  final override def annotate(psiElement: PsiElement, annotationHolder: AnnotationHolder): Unit =
    list.foreach(_.annotate(psiElement, annotationHolder))
  end annotate
end Annotators

