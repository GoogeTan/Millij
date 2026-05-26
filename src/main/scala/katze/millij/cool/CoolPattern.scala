package katze.millij.cool

import cats.Id
import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.{ElementPattern, StandardPatterns}
import com.intellij.psi.PsiElement
import katze.millij.psi.PsiChild

import scala.Tuple.:*
import scala.util.NotGiven

/**
 * A way to express ElementPatterns that produce values. Other Cool-prefixed classes take them as params instead of ElementPatterns
 * Usually it is a typecheck of PsiElement and/or it's parents.
 * @param pattern tests if value can be built
 * @param extract builds value from PsiElement
 */
final case class CoolPattern[Data](
  pattern : ElementPattern[? <: PsiElement],
  extract : PsiElement => Option[Data]
):
  def map[Data2](f : Data => Data2) : CoolPattern[Data2] =
    CoolPattern(
      pattern,
      element =>
        extract(element).map(f)
    )
  end map

  def mapOption[Data2](f : Data => Option[Data2]) : CoolPattern[Data2] =
    CoolPattern(
      pattern,
      element =>
        extract(element).flatMap(f)
    )
  end mapOption

  def collect[Data2](f : PartialFunction[Data, Data2]) : CoolPattern[Data2] =
    CoolPattern(
      pattern,
      element =>
        extract(element).collect(f)
    )
  end collect

  def &&[Data2](el: CoolPattern[Data2])(using NotGiven[Data <:< Tuple]): CoolPattern[(Data, Data2)] =
    CoolPattern(
      StandardPatterns.and[PsiElement](
        pattern,
        el.pattern
      ),
      element =>
        extract(element)
          .zip(el.extract(element))
    )
  end &&

  def :*[Data1 <: Tuple, Data2](el: CoolPattern[Data2])(using dataISTuple : Data <:< Data1): CoolPattern[Data1 :* Data2] =
    CoolPattern(
      StandardPatterns.and[PsiElement](
        pattern,
        el.pattern
      ),
      element =>
        extract(element)
          .zip(el.extract(element))
          .map((data1, data2) => dataISTuple.substituteCo[Id](data1) :* data2)
    )
  end :*

  def *:[Data1 <: Tuple, Data2](el: CoolPattern[Data2])(using dataISTuple : Data <:< Data1): CoolPattern[Data2 *: Data1] =
    CoolPattern(
      StandardPatterns.and[PsiElement](
        pattern,
        el.pattern
      ),
      element =>
        extract(element)
          .zip(el.extract(element))
          .map((data1, data2) => data2 *: dataISTuple.substituteCo[Id](data1))
    )
  end *:

  def ||[Data2](el: CoolPattern[Data2]): CoolPattern[Data | Data2] =
    CoolPattern(
      StandardPatterns.or[PsiElement](
        pattern,
        el.pattern
      ),
      element =>
        extract(element).orElse(el.extract(element))
    )
  end ||
end CoolPattern

object CoolPattern:
  def elementAndParent[
    Element <: PsiElement : PsiElementMatcher as PEM,
    Parent : PsiParentElementMatcher as PP,
  ](level : Int = 1) : CoolPattern[(Element, Parent)] =
    element && parent(level)
  end elementAndParent
  
  def elementAndParents[
    Element <: PsiElement : PsiElementMatcher as PEM,
    Parent <: Tuple : PsiParentElementMatcher as PP,
  ](level : Int = 1) : CoolPattern[Element *: Parent] =
    element[Element] *: parent[Parent](level)
  end elementAndParents
  
  def element[Element <: PsiElement : PsiElementMatcher as PEM] : CoolPattern[Element] =
    CoolPattern(
      PEM.capture,
      PEM.extract
    )
  end element

  def parent[Parent : PsiParentElementMatcher as PP](level : Int = 1) : CoolPattern[Parent] =
    CoolPattern(
      PP.appendTo(
        psiElement(),
        level
      ),
      element =>
        PP.test(PsiChild.unapplySeq(element).apply(level))//TODO extract efficiently
    )
  end parent
end CoolPattern