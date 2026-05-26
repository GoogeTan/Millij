package katze.millij.reference.cool

import com.intellij.patterns.{ElementPattern, PsiElementPattern}
import com.intellij.psi.{PsiElement, PsiReference, PsiReferenceProvider, PsiReferenceRegistrar}
import com.intellij.util.ProcessingContext
import katze.millij.cool.{PsiElementMatcher, PsiParentElementMatcher}

extension(psiReferenceRegistrar : PsiReferenceRegistrar)
  final def registerCoolReferenceProvider[
    Element <: PsiElement : PsiElementMatcher as pem,
    Parents  : PsiParentElementMatcher as psiParents
  ](
    coolPsiReferenceProvider: CoolPsiReferenceProvider[Element, Parents],
    place: PsiElementPattern.Capture[Element] => ElementPattern[? <: PsiElement]
  ) : Unit =
    psiReferenceRegistrar.registerReferenceProvider(
      place(psiParents.appendTo(pem.capture)),
      (psiElement: PsiElement, processingContext: ProcessingContext) =>
        (pem.extract(psiElement), psiParents.test(psiElement.getParent)) match
          case (Some(element), Some(parent)) =>
            coolPsiReferenceProvider(element, parent, processingContext).toArray
          case _ =>
            //TODO add logging
            Array.empty
    )
  end registerCoolReferenceProvider
end extension