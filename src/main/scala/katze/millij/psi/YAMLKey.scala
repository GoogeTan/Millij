package katze.millij.psi

import com.intellij.patterns.{PatternCondition, PlatformPatterns, PsiElementPattern}
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import katze.millij.cool.{PsiElementMatcher, PsiParent}
import org.jetbrains.yaml.psi.YAMLKeyValue

import scala.reflect.{ClassTag, TypeTest, classTag}

opaque type YAMLKey[T <: PsiElement] <: T = T

object YAMLKey:
  def unapply[T <: PsiElement](value : T) : Option[YAMLKey[T]] =
    value.getParent match
      case kv : YAMLKeyValue if kv.getKey == value => Some(value)
      case _ => None
  end unapply
  
  given valueNodeMatcher[T <: PsiElement](
    using
    ct: ClassTag[T],
    tt: TypeTest[PsiElement, T]
  ): PsiElementMatcher[YAMLKey[T]] with

    override def capture: PsiElementPattern.Capture[YAMLKey[T]] =
      PlatformPatterns.psiElement(ct.runtimeClass.asInstanceOf[Class[T]])
        .`with`(new PatternCondition[T]("isStrictlyTheKeyNode") {
          override def accepts(element: T, context: ProcessingContext): Boolean =
            element.getParent match
              case kv: YAMLKeyValue => kv.getKey == element
              case _ => false
        })
    end capture

    override def extract[K <: PsiElement](element: K): Option[element.type & YAMLKey[T]] =
      element match
        case t: T =>
          t.getParent match
            case kv: YAMLKeyValue if kv.getKey == t =>
              Some(t.asInstanceOf[element.type & YAMLKey[T]])
            case _ =>
              None
        case _ =>
          None
    end extract
  end valueNodeMatcher

  given valueNodePsiParent[T <: PsiElement : ClassTag](using tt: TypeTest[PsiElement, T]): PsiParent[YAMLKey[T]] with

    override def appendTo[V <: PsiElement](
      value: PsiElementPattern.Capture[V],
      level: Int
    ): PsiElementPattern.Capture[V] =
      value.withSuperParent(
        level,
        PlatformPatterns.psiElement(classTag[T].runtimeClass.asInstanceOf[Class[T]])
          .`with`(new PatternCondition[T]("isStrictlyTheKeyNode") {
            override def accepts(element: T, context: ProcessingContext): Boolean =
              element.getParent match
                case kv: YAMLKeyValue => kv.getKey == element
                case _ => false
          })
      )
    end appendTo

    override def test(value: PsiElement): Option[YAMLKey[T]] =
      tt.unapply(value).filter { element =>
        element.getParent match
          case kv: YAMLKeyValue => kv.getKey == element
          case _ => false
      }.map(_.asInstanceOf[YAMLKey[T]])
    end test

  end valueNodePsiParent
end YAMLKey

