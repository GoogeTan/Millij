package katze.millij.inlay

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.{PsiElement, PsiFile}
import katze.millij.place.{PlaceConfigResolver, PlaceInYamlConfig, isExtendsBlock, richScopeOf, isObjectDeclarationText}
import katze.millij.psi.{YAMLKey, YAMLKeyValueWithNotKey}
import katze.millij.psi.{YAMLChild, YAMLGrandChild}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager
import org.jetbrains.yaml.psi.{YAMLKeyValue, YAMLMapping, YAMLScalar}
import katze.millij.scalatypes.classTypeSearch

//TODO исправить высокую сложность сего файла
final class MillYamlInlayHintsProvider extends InlayHintsProvider:
  override def createCollector(psiFile: PsiFile, editor: Editor): InlayHintsCollector =
    Collector

  object Collector extends SharedBypassCollector:
    override def collectFromElement(
      psiElement: PsiElement,
      inlayTreeSink: InlayTreeSink
    ): Unit =
      val (fieldName, parentScope) = psiElement match
        case YAMLChild(scalar : YAMLScalar, mapping : YAMLMapping) =>
          (scalar.getTextValue, richScopeOf(scalar))
        case YAMLGrandChild(YAMLKey(self), kv : YAMLKeyValue, mapping : YAMLMapping) if !isExtendsBlock(kv.getKeyText) && !isObjectDeclarationText(kv.getKeyText) =>
          (kv.getKeyText, richScopeOf(kv))
        case _ =>
          return
      val maybeScope = parentScope.flatMap(parentScope =>
          val project = psiElement.getProject
          val search = (text :String) =>
            Right(classTypeSearch(ScalaPsiManager.instance(project), GlobalSearchScope.allScope(project), text))
          PlaceConfigResolver(search).field(
            parentScope,
            fieldName
          )
        )
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
