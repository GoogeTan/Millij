package katze.millij.inlay

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.{PsiElement, PsiFile}
import katze.millij
import katze.millij.psi.{YAMLKey, YAMLKeyValueWithNotKey}
import katze.millij.psi.{YAMLChild, YAMLGrandChild}
import katze.millij.{PlaceInYamlConfig, isExtendsBlock, richScopeOf}
import org.jetbrains.yaml.psi.{YAMLKeyValue, YAMLMapping, YAMLScalar}

final class MillYamlInlayHintsProvider extends InlayHintsProvider:
  override def createCollector(psiFile: PsiFile, editor: Editor): InlayHintsCollector =
    Collector

  object Collector extends SharedBypassCollector:
    override def collectFromElement(
      psiElement: PsiElement,
      inlayTreeSink: InlayTreeSink
    ): Unit =
      val maybeScope = psiElement match
        case YAMLChild(scalar : YAMLScalar, mapping : YAMLMapping) =>
          richScopeOf(scalar)
        case YAMLGrandChild(YAMLKey(self), kv : YAMLKeyValue, mapping : YAMLMapping) if kv.getValue != null && !isExtendsBlock(kv.getKeyText) =>
          //TODO сделать поддержку, если значения нет(но мы знаем его тип всё ещё по идее)
          richScopeOf(kv.getValue)
        case _ =>
          return
      val scope = maybeScope.fold(
        error =>
          println(s"Inlay rich scope error: ${error}")
          return,
        identity
      )
      scope match
        case PlaceInYamlConfig.Module(_, _) =>
        case PlaceInYamlConfig.Member(_, _, expectedType, _) =>
          inlayTreeSink.addPresentation(
            InlineInlayPosition(psiElement.getTextRange.getEndOffset, true, 0),
            null,
            null,
            HintFormat(
              HintColorKind.Default,
              HintFontSize.AsInEditor,
              HintMarginPadding.OnlyPadding
            ),
            builder =>
              builder.text(s": ${expectedType}", null)
              kotlin.Unit.INSTANCE
          )
    end collectFromElement
  end Collector
end MillYamlInlayHintsProvider
