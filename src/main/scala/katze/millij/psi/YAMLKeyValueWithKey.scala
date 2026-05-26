package katze.millij.psi

import com.intellij.patterns.{PatternCondition, PlatformPatterns, PsiElementPattern}
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import katze.millij.cool.{PsiElementMatcher, PsiParentElementMatcher}
import org.jetbrains.yaml.psi.YAMLKeyValue

/**
 * YAMLKeyValue that getKeyText == expectedKey
 */
opaque type YAMLKeyValueWithKey[Key <: String] <: YAMLKeyValue = YAMLKeyValue

object YAMLKeyValueWithKey:
  given keyValuePairWithMatcher[Key <: String](using valueOf: ValueOf[Key]): PsiElementMatcher[YAMLKeyValueWithKey[Key]] with

    private val expectedKey: String = valueOf.value

    override def capture: PsiElementPattern.Capture[YAMLKeyValueWithKey[Key]] =
      PlatformPatterns.psiElement(classOf[YAMLKeyValue])
        .`with`(new PatternCondition[YAMLKeyValue](s"hasKeyName($expectedKey)") {
          override def accepts(kv: YAMLKeyValue, context: ProcessingContext): Boolean =
            kv.getKeyText == expectedKey
        })
        .asInstanceOf[PsiElementPattern.Capture[YAMLKeyValueWithKey[Key]]]

    override def extract[K <: PsiElement](element: K): Option[element.type & YAMLKeyValueWithKey[Key]] =
      element match
        case kv: YAMLKeyValue if kv.getKeyText == expectedKey =>
          Some(kv.asInstanceOf[element.type & YAMLKeyValueWithKey[Key]])
        case _ =>
          None
      end match
    end extract
  end keyValuePairWithMatcher

  given keyValuePairWithParentElementMatcher[Key <: String](using valueOf: ValueOf[Key]): PsiParentElementMatcher[YAMLKeyValueWithKey[Key]] with
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
              kv.getKeyText == expectedKey
          })
      )

    override def test(value: PsiElement): Option[YAMLKeyValueWithKey[Key]] =
      value match
        case kv: YAMLKeyValue if kv.getKeyText == expectedKey =>
          Some(kv)
        case _ =>
          None
  end keyValuePairWithParentElementMatcher
end YAMLKeyValueWithKey

