package katze.millij.cool

import com.intellij.patterns.{PsiElementPattern, StandardPatterns}
import com.intellij.psi.PsiElement

import scala.reflect.{ClassTag, TypeTest, classTag}

trait PsiParent[T]:
  def appendTo[V <: PsiElement](
    value : PsiElementPattern.Capture[V],
    level : Int = 1
  ) : PsiElementPattern.Capture[V]

  //TODO replace with TypeTest
  def test(value : PsiElement) : Option[T]
end PsiParent

object PsiParent:
  given clazzPsiParent[Element <: PsiElement : ClassTag](using tt: TypeTest[PsiElement, Element]) : PsiParent[Element] with
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
      tt.unapply(value)
    end test
  end clazzPsiParent

  given unionPsiParent[
    Element1 : PsiParent as EPP1,
    Element2 : PsiParent as EPP2,
  ]: PsiParent[Either[Element1, Element2]]
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
      EPP1.test(value).map(Left(_)).orElse(EPP2.test(value).map(Right(_)))
    end test
  end unionPsiParent


  given inductivePsiParent[
    Head : PsiParent as HPP,
    Tail <: Tuple : PsiParent as TCT
  ]: PsiParent[Head *: Tail] with
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
      HPP.test(value).zip(TCT.test(value.getParent)).map(_ *: _)
    end test
  end inductivePsiParent

  given leafPsiParent: PsiParent[EmptyTuple] with
    override def appendTo[V <: PsiElement](
      value: PsiElementPattern.Capture[V],
      level: Int
    ): PsiElementPattern.Capture[V] =
      value
    end appendTo

    override def test(value: PsiElement): Option[EmptyTuple] =
      Some(EmptyTuple)
    end test
  end leafPsiParent
end PsiParent
