package katze.millij.psi

import com.intellij.psi.PsiElement

object PsiChild:
  def unapplySeq(psiElement : PsiElement) : Seq[PsiElement] =
    psiElement +: LazyList.unfold(psiElement)(
      element => Option(element.getParent).map(a => (a, a))
    )
  end unapplySeq
end PsiChild
