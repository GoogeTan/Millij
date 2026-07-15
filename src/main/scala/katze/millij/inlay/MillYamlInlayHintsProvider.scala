package katze.millij.inlay

import com.intellij.codeInsight.hints.declarative.*
import com.intellij.openapi.editor.Editor
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.{PsiElement, PsiFile}
import katze.millij.data.*
import katze.millij.data.module.NamespacedPath
import katze.millij.file.*
import katze.millij.place.*
import katze.millij.psi.{YAMLChild, YAMLGrandChild, YAMLKey, YAMLKeyValueWithNotKey}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.yaml.psi.{YAMLKeyValue, YAMLMapping, YAMLScalar}

/**
 * Adds type inlay hints for members.
 */
final class MillYamlInlayHintsProvider extends InlayHintsProvider:
  override def createCollector(psiFile: PsiFile, editor: Editor): InlayHintsCollector | Null =
    val project = psiFile.getProject
    Smart(project) {
      psiFile.getVirtualFile
        .relativePathToContentRoot(project)
        .map(SegmentedPath.fromPath)
        .flatMap(_.traverse(ScalaIdentifier.fromStringOption))
        .map(rootPath =>
          Collector(richPlaceConfigResolverOption(project, rootPath))
        ).orNull
    }.orNull
  end createCollector

  class Collector(
    resolver : YAMLConfigResolver[Option, PlaceInYamlConfig[ScType]]
  )(using Smart) extends SharedBypassCollector:
    override def collectFromElement(
      psiElement: PsiElement,
      inlayTreeSink: InlayTreeSink
    ): Unit =
      extractElement(psiElement).flatMap(innerScopeOf).getOrElse(return) match
        case PlaceInYamlConfig.Module(_, _, _) =>
        case PlaceInYamlConfig.Member(_, expectedType, _) =>
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
              builder.text(s": $expectedType", null)
              kotlin.Unit.INSTANCE
          )
    end collectFromElement

    /**
     * Tests element for being suitable for making an inlay for it and returns an element for which document place is defined.
     * Inlays will be made for keys of key value pairs which define members(not modules or extends blocks) and scalars which are unfinished key value pairs:
     *  ```YAML
     * extends: SomeModule
     * unfinishedKeypairThatIsParsedAsScalar
     * someKeyValueExample:
     *  nestedKeyValue: "123"
     *  nestedUnfinishedKeyValueThatIsParsedAsScalar
     * ```
     * Here the method will return [[Some]] for `unfinishedKeypairThatIsParsedAsScalar`, `someKeyValueExample`(scalar inside key value pair, not the pair itself),
     * `nestedKeyValue`(again, as a key), `nestedUnfinishedKeyValueThatIsParsedAsScalar`. For `extends`, `nestedKeyValue:`, etc. it will return [[None]].
     */
    def extractElement : PsiElement => Option[YAMLScalar | YAMLKeyValue] =
      case YAMLChild(scalar: YAMLScalar, _: YAMLMapping) =>
        Some(scalar)
      case YAMLGrandChild(YAMLKey(_), kv: YAMLKeyValue, _: YAMLMapping) if !isExtendsBlock(kv.getKeyText) && !isObjectDeclarationText(kv.getKeyText)  =>
        Some(kv)
      case _ =>
        None
    end extractElement

    /**
     * Place where given element is defined. It expects element that encodes member name in mapping:
     *  ```YAML
     * extends: SomeModule
     * unfinishedKeypairThatIsParsedAsScalar
     * someKeyValueExample:
     *  nestedKeyValue: "123"
     *  nestedUnfinishedKeyValueThatIsParsedAsScalar
     *
     * ```
     * For `unfinishedKeypairThatIsParsedAsScalar` and `someKeyValueExample` it will return [[PlaceInYamlConfig.Module]] of SomeModule.
     * For `nestedKeyValue` and `nestedUnfinishedKeyValueThatIsParsedAsScalar` it will return [[PlaceInYamlConfig.Member]] of [[PlaceInYamlConfig.Module]] of SomeModule.
     */
    def enclosingScope(using Smart) : YAMLScalar | YAMLKeyValue => Option[(String, PlaceInYamlConfig[ScType])] =
      case scalar: YAMLScalar =>
        richPlaceOf(scalar).toOption.map((scalar.getTextValue, _))
      case kv: YAMLKeyValue =>
        richPlaceOf(kv).toOption.map((kv.getKeyText, _))
      case _ =>
        None
    end enclosingScope

    /**
     * Place inside the given element. It expects element that encodes member name in mapping:
     *  ```YAML
     * extends: SomeModule
     * unfinishedKeypairThatIsParsedAsScalar
     * someKeyValueExample: "text"
     * someKeyValueExampleWithoutBody:
     * ```
     * For all `unfinishedKeypairThatIsParsedAsScalar`, `someKeyValueExample`, `someKeyValueExampleWithoutBody`
     * it will return scope of member with corresponding name in SomeModule.
     */
    def innerScopeOf(element : YAMLScalar | YAMLKeyValue)(using Smart) : Option[PlaceInYamlConfig[ScType]] =
      enclosingScope(element).flatMap((fieldName, parentScope) =>
        resolver.field(parentScope, fieldName)
      )
    end innerScopeOf
  end Collector
end MillYamlInlayHintsProvider
