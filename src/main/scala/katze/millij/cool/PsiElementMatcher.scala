package katze.millij.cool

import com.intellij.patterns.{PlatformPatterns, PsiElementPattern, StandardPatterns}
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.{PsiElement, PsiErrorElement, PsiWhiteSpace}
import katze.millij.psi.CompletionPosition

import scala.reflect.{ClassTag, TypeTest}

trait PsiElementMatcher[T <: PsiElement]:
  def capture: PsiElementPattern.Capture[T]
  def extract[K <: PsiElement](element: K): Option[element.type & T]
end PsiElementMatcher

object PsiElementMatcher:
  given defaultMatcher[T <: PsiElement](using ct: ClassTag[T], tt: TypeTest[PsiElement, T]): PsiElementMatcher[T] with
    override def capture: PsiElementPattern.Capture[T] =
      PlatformPatterns.psiElement(ct.runtimeClass.asInstanceOf[Class[T]])

    override def extract[K <: PsiElement](element: K): Option[element.type & T] =
      tt.unapply(element)
    end extract
  end defaultMatcher

  given PsiElementMatcher[CompletionPosition] with
    override def capture: PsiElementPattern.Capture[CompletionPosition] =
      PlatformPatterns.psiElement(classOf[PsiElement])
        .and(
          StandardPatterns.or(
            PlatformPatterns.psiElement(classOf[LeafPsiElement]),
            PlatformPatterns.psiElement(classOf[PsiWhiteSpace]),
            PlatformPatterns.psiElement(classOf[PsiErrorElement])
          )
        ).asInstanceOf[PsiElementPattern.Capture[CompletionPosition]]

    override def extract[K <: PsiElement](element: K): Option[element.type & CompletionPosition] =
      element match
        case e: LeafPsiElement  => Some(e.asInstanceOf[element.type & LeafPsiElement])
        case e: PsiWhiteSpace   => Some(e.asInstanceOf[element.type & PsiWhiteSpace])
        case e: PsiErrorElement => Some(e.asInstanceOf[element.type & PsiErrorElement])
        case _                  => None
    end extract
  end given
end PsiElementMatcher
