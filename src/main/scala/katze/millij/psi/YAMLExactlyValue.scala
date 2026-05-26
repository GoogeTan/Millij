package katze.millij.psi

import com.intellij.patterns.{PatternCondition, PlatformPatterns, PsiElementPattern}
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import katze.millij.cool.{PsiElementMatcher, PsiParentElementMatcher}
import org.jetbrains.yaml.psi.{YAMLKeyValue, YAMLValue}

import scala.reflect.{ClassTag, TypeTest, classTag}

/**
 * A value psi element of YAMLKeyValue.
 * In other words it is true that element.getParent is YAMLKeyValue and element.getParent.getValue is this
 */
opaque type YAMLExactlyValue[T <: PsiElement] <: T = T

object YAMLExactlyValue:
  given valueNodeMatcher[T <: PsiElement](
    using
    ct: ClassTag[T],
    tt: TypeTest[PsiElement, T]
  ): PsiElementMatcher[YAMLExactlyValue[T]] with

    override def capture: PsiElementPattern.Capture[YAMLExactlyValue[T]] =
      PlatformPatterns.psiElement(ct.runtimeClass.asInstanceOf[Class[T]])
        .`with`(new PatternCondition[T]("isStrictlyTheValueNode") {
          override def accepts(element: T, context: ProcessingContext): Boolean =
            element.getParent match
              case kv: YAMLKeyValue => kv.getValue == element
              case _ => false
        })
        .asInstanceOf[PsiElementPattern.Capture[YAMLExactlyValue[T]]]

    override def extract[K <: PsiElement](element: K): Option[element.type & YAMLExactlyValue[T]] =
      element match
        case t: T =>
          t.getParent match
            case kv: YAMLKeyValue if kv.getValue == t =>
              Some(t.asInstanceOf[element.type & YAMLExactlyValue[T]])
            case _ =>
              None
        case _ =>
          None
    end extract
  end valueNodeMatcher

  given valueNodePsiParentElementMatcher[T <: PsiElement : ClassTag](using tt: TypeTest[PsiElement, T]): PsiParentElementMatcher[YAMLExactlyValue[T]] with
    override def appendTo[V <: PsiElement](
      value: PsiElementPattern.Capture[V],
      level: Int
    ): PsiElementPattern.Capture[V] =
      value.withSuperParent(
        level,
        PlatformPatterns.psiElement(classTag[T].runtimeClass.asInstanceOf[Class[T]])
          .`with`(new PatternCondition[T]("isStrictlyTheValueNode") {
            override def accepts(element: T, context: ProcessingContext): Boolean =
              element.getParent match
                case kv: YAMLKeyValue => kv.getValue == element
                case _ => false
          })
      )
    end appendTo

    override def test(value: PsiElement): Option[YAMLExactlyValue[T]] =
      tt.unapply(value).filter { element =>
        element.getParent match
          case kv: YAMLKeyValue => kv.getValue == element
          case _ => false
      }.map(_.asInstanceOf[YAMLExactlyValue[T]])
    end test

  end valueNodePsiParentElementMatcher
end YAMLExactlyValue
