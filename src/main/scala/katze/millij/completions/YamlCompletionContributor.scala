package katze.millij.completions

import com.intellij.codeInsight.completion.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.patterns.{PlatformPatterns, StandardPatterns}
import com.intellij.psi.PsiElement
import katze.millij.completions.cool.CoolCompletionContributor
import katze.millij.completions.providers.*
import katze.millij.cool.{CoolPattern, PsiParent}
import katze.millij.psi.CompletionPosition
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.{YAMLKeyValue, YAMLPsiElement, YAMLScalar, YAMLSequenceItem}

//TODO split into dumb aware and not dumb aware versions
final class YamlCompletionContributor extends CoolCompletionContributor:
  val logger: Logger = Logger.getInstance(classOf[YamlCompletionContributor])

  coolExtend(
    CompletionType.BASIC,
    extendsExistingKeyCompletionProvider,
    _
      .withLanguage(YAMLLanguage.INSTANCE)
      .inVirtualFile(
        PlatformPatterns.virtualFile().withName(
          StandardPatterns.string().endsWith(".mill.yaml")
        )
      )
  )

  coolExtend(
    CompletionType.BASIC,
    extendsNewKeyCompletionProvider,
    _
      .withLanguage(YAMLLanguage.INSTANCE)
      .inVirtualFile(
        PlatformPatterns.virtualFile().withName(
          StandardPatterns.string().endsWith(".mill.yaml")
        )
      )
  )

  coolExtend(
    CompletionType.BASIC,
    extendsValueCompletionProvider,
    _
      .withLanguage(YAMLLanguage.INSTANCE)
      .inVirtualFile(
        PlatformPatterns.virtualFile().withName(
          StandardPatterns.string().endsWith(".mill.yaml")
        )
      )
  )

  coolExtend(
    CompletionType.BASIC,
    extendsListCompletionProvider,
    _
      .withLanguage(YAMLLanguage.INSTANCE)
      .inVirtualFile(
        PlatformPatterns.virtualFile().withName(
          StandardPatterns.string().endsWith(".mill.yaml")
        )
      )
  )

  coolExtend(
    CompletionType.BASIC,
    memberCompletionProvider(logger),
    _
      .withLanguage(YAMLLanguage.INSTANCE)
      .inVirtualFile(
        PlatformPatterns.virtualFile().withName(
          StandardPatterns.string().endsWith(".mill.yaml")
        )
      )
  )

  coolExtend(
    CompletionType.BASIC,
    scalaVersionCompletionProvider,
    _
      .withLanguage(YAMLLanguage.INSTANCE)
      .inVirtualFile(
        PlatformPatterns.virtualFile().withName(
          StandardPatterns.string().endsWith(".mill.yaml")
        )
      ),
  )

  patternExtend(
    CompletionType.BASIC,
    CoolPattern.elementAndParent[CompletionPosition, YAMLPsiElement]() :* yamlMavenDependenciesPattern,
    _
      .withLanguage(YAMLLanguage.INSTANCE)
      .inVirtualFile(
        PlatformPatterns.virtualFile().withName(
          StandardPatterns.string().endsWith(".mill.yaml")
        )
      ),
  ) {
    case ((_, parent, text), ctx, resultSet) =>
      suggestMavenDependency(parent, text, resultSet)
  }
end YamlCompletionContributor
