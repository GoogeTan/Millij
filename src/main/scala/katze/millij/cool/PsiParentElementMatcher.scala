package katze.millij.cool

import com.intellij.patterns.{PsiElementPattern, StandardPatterns}
import com.intellij.psi.PsiElement
import katze.millij.psi.CompletionPosition

import scala.reflect.{ClassTag, TypeTest, classTag}

/**
 * Allows to match over parents of PsiElementPattern.Capture with extraction of data of type T. 
 * 
 * Usually T it is some PsiElement which we expect to be a parent at some level. For complex data extraction use
 * [[CoolPattern]]
 */
trait PsiParentElementMatcher[T]:
  def appendTo[V <: PsiElement](
    value : PsiElementPattern.Capture[V],
    level : Int = 1
  ) : PsiElementPattern.Capture[V]
  
  def test(value : PsiElement) : Option[T]
end PsiParentElementMatcher

object PsiParentElementMatcher:
  given clazzPsiParentElementMatcher[Element <: PsiElement : ClassTag](using tt: TypeTest[PsiElement, Element]) : PsiParentElementMatcher[Element] with
    override def appendTo[V <: PsiElement](
      value: PsiElementPattern.Capture[V],
      level: Int
    ): PsiElementPattern.Capture[V] =
      value.withSuperParent(
        level,
        classTag[Element].runtimeClass.asInstanceOf[Class[Element]]
      )
    end appendTo

    override def test(value: PsiElement): Option[Element] =
      if value == null then None else tt.unapply(value)
    end test
  end clazzPsiParentElementMatcher

  given unionPsiParentElementMatcher[
    Element1 : PsiParentElementMatcher as EPP1,
    Element2 : PsiParentElementMatcher as EPP2,
  ]: PsiParentElementMatcher[Either[Element1, Element2]]
   with
    override def appendTo[V <: PsiElement](
      value: PsiElementPattern.Capture[V],
      level: Int
    ): PsiElementPattern.Capture[V] =
      value.and(
        StandardPatterns.or(
          EPP1.appendTo(
            value,
            level
          ),
          EPP2.appendTo(
            value,
            level
          )
        )
      )
    end appendTo

    override def test(value: PsiElement): Option[Either[Element1, Element2]] =
      if value == null then None
      else EPP1.test(value).map(Left(_)).orElse(EPP2.test(value).map(Right(_)))
    end test
  end unionPsiParentElementMatcher


  given inductivePsiParentElementMatcher[
    Head : PsiParentElementMatcher as HPP,
    Tail <: Tuple : PsiParentElementMatcher as TCT
  ]: PsiParentElementMatcher[Head *: Tail] with
    override def appendTo[V <: PsiElement](
      value: PsiElementPattern.Capture[V],
      level : Int
    ): PsiElementPattern.Capture[V] =
      TCT.appendTo(
        HPP.appendTo(
          value,
          level
        ),
        level + 1
      )
    end appendTo

    override def test(value: PsiElement): Option[Head *: Tail] =
      if value == null then None
      else HPP.test(value).zip(TCT.test(value.getParent)).map(_ *: _)
    end test
  end inductivePsiParentElementMatcher

  given leafPsiParentElementMatcher: PsiParentElementMatcher[EmptyTuple] with
    override def appendTo[V <: PsiElement](
      value: PsiElementPattern.Capture[V],
      level: Int
    ): PsiElementPattern.Capture[V] =
      value
    end appendTo

    override def test(value: PsiElement): Option[EmptyTuple] =
      Some(EmptyTuple)
    end test
  end leafPsiParentElementMatcher
end PsiParentElementMatcher
