package katze.millij.psi

import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.StandardPatterns.or
import com.intellij.patterns.{PatternCondition, PsiElementPattern}
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import katze.millij.cool.PsiParentElementMatcher
import katze.millij.place.isObjectDeclarationText
import org.jetbrains.yaml.psi.{YAMLDocument, YAMLKeyValue}

/**
 * A mill module declaration. It is either YAMLDocument or YAMLKeyValue with name "object ...".
 */
opaque type YAMLMillModule <: YAMLDocument | YAMLKeyValue = YAMLDocument | YAMLKeyValue

object YAMLMillModule:
  given ModulePsiParentElementMatcher : PsiParentElementMatcher[YAMLMillModule] with
    override def appendTo[V <: PsiElement](value: PsiElementPattern.Capture[V], level: Int): PsiElementPattern.Capture[V] =
      value.withSuperParent(
        level,
        or(
          psiElement(classOf[YAMLDocument]),
          psiElement(classOf[YAMLKeyValue]).`with`(ModulePatternCondition),
        )
      )
    end appendTo

    override def test(value: PsiElement): Option[YAMLMillModule] =
      value match
        case d : YAMLDocument => Some(d)
        case kv : YAMLKeyValue if isObjectDeclarationText(kv.getKeyText) => Some(kv)
        case _ => None
      end match
    end test
  end ModulePsiParentElementMatcher

  object ModulePatternCondition extends PatternCondition[YAMLKeyValue]("yamlMillModuleKvTester"):
    override def accepts(t: YAMLKeyValue, processingContext: ProcessingContext): Boolean =
      isObjectDeclarationText(t.getKeyText)
    end accepts
  end ModulePatternCondition
end YAMLMillModule