package katze.millij.psi

import cats.syntax.all.*
import com.intellij.patterns.{PatternCondition, PlatformPatterns, PsiElementPattern}
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import katze.millij.cool.{PsiElementMatcher, PsiParentElementMatcher}
import katze.millij.psi.YAMLKeyValueWithNotKey as keyValuePairWithMatcher
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * YAMLKeyValue that getKeyText != expectedKey
 */
opaque type YAMLKeyValueWithNotKey[Key <: String] <: YAMLKeyValue = YAMLKeyValue

object YAMLKeyValueWithNotKey:
  def unapply[Key <: String : ValueOf](value : YAMLKeyValue) : Option[YAMLKeyValueWithNotKey[Key]] =
    keyValuePairWithMatcher.extract(value)
  end unapply 
  
  given keyValuePairWithMatcher[Key <: String](using valueOf: ValueOf[Key]): PsiElementMatcher[YAMLKeyValueWithNotKey[Key]] with

    private val expectedKey: String = valueOf.value

    override def capture: PsiElementPattern.Capture[YAMLKeyValueWithNotKey[Key]] =
      PlatformPatterns.psiElement(classOf[YAMLKeyValue])
        .`with`(new PatternCondition[YAMLKeyValue](s"hasNotKeyName($expectedKey)") {
          override def accepts(kv: YAMLKeyValue, context: ProcessingContext): Boolean =
            kv.getKeyText =!= expectedKey
        })
        .asInstanceOf[PsiElementPattern.Capture[YAMLKeyValueWithNotKey[Key]]]

    override def extract[K <: PsiElement](element: K): Option[element.type & YAMLKeyValueWithNotKey[Key]] =
      element match
        case kv: YAMLKeyValue if kv.getKeyText =!= expectedKey =>
          Some(kv.asInstanceOf[element.type & YAMLKeyValueWithNotKey[Key]])
        case _ =>
          None
      end match
    end extract
  end keyValuePairWithMatcher

  given keyValuePairWithParentElementMatcher[Key <: String](using valueOf: ValueOf[Key]): PsiParentElementMatcher[YAMLKeyValueWithNotKey[Key]] with
    private val expectedKey: String = valueOf.value

    override def appendTo[V <: PsiElement](
      value: PsiElementPattern.Capture[V],
      level: Int
    ): PsiElementPattern.Capture[V] =
      value.withSuperParent(
        level,
        PlatformPatterns.psiElement(classOf[YAMLKeyValue])
          .`with`(new PatternCondition[YAMLKeyValue](s"hasKeyName($expectedKey)") {
            override def accepts(kv: YAMLKeyValue, context: ProcessingContext): Boolean =
              kv.getKeyText =!= expectedKey
          })
      )

    override def test(value: PsiElement): Option[YAMLKeyValueWithNotKey[Key]] =
      value match
        case kv: YAMLKeyValue if kv.getKeyText =!= expectedKey =>
          Some(kv)
        case _ =>
          None
  end keyValuePairWithParentElementMatcher
end YAMLKeyValueWithNotKey

