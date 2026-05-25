package katze.millij.psi

import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.StandardPatterns.or
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.{PsiElement, PsiErrorElement, PsiWhiteSpace}

type CompletionPosition = LeafPsiElement | PsiWhiteSpace | PsiErrorElement

object CompletionPosition:
  def unapply(element : PsiElement) : Option[CompletionPosition & element.type] =
    element match
      case a : LeafPsiElement => Some(a.asInstanceOf[LeafPsiElement & element.type])
      case a : PsiWhiteSpace => Some(a.asInstanceOf[PsiWhiteSpace & element.type])
      case a : PsiErrorElement => Some(a.asInstanceOf[PsiErrorElement & element.type])
      case _ => None
  end unapply

  val autocompletePattern =
    psiElement().and(
      or(
        psiElement(classOf[LeafPsiElement]),
        psiElement(classOf[PsiWhiteSpace]),
        psiElement(classOf[PsiErrorElement])
      )
    )
end CompletionPosition
