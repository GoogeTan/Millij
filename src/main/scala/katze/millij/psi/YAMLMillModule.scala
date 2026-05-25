package katze.millij.psi

import com.intellij.patterns.PlatformPatterns.psiElement
import com.intellij.patterns.StandardPatterns.or
import com.intellij.patterns.{PatternCondition, PsiElementPattern}
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import katze.millij.cool.PsiParent
import katze.millij.isObjectDeclarationText
import org.jetbrains.yaml.psi.{YAMLDocument, YAMLKeyValue}

opaque type YAMLMillModule <: YAMLDocument | YAMLKeyValue = YAMLDocument | YAMLKeyValue

object YAMLMillModule:
  given ModulePsiParent : PsiParent[YAMLMillModule] with
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
  end ModulePsiParent

  object ModulePatternCondition extends PatternCondition[YAMLKeyValue]("yamlMillModuleKvTester"):
    override def accepts(t: YAMLKeyValue, processingContext: ProcessingContext): Boolean =
      isObjectDeclarationText(t.getKeyText)
    end accepts
  end ModulePatternCondition
end YAMLMillModule