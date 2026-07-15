package katze.millij.completions

import com.intellij.codeInsight.completion.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.patterns.{PlatformPatterns, StandardPatterns}
import com.intellij.psi.PsiElement
import katze.millij.completions.cool.{CoolCompletionContributor, DumbCoolCompletionContributor, SmartCoolCompletionContributor}
import katze.millij.completions.providers.*
import katze.millij.cool.{CoolPattern, PsiParentElementMatcher}
import katze.millij.psi.CompletionPosition
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.{YAMLKeyValue, YAMLPsiElement, YAMLScalar, YAMLSequenceItem}

final class SmartYamlCompletionContributor extends SmartCoolCompletionContributor:
  val logger: Logger = Logger.getInstance(classOf[SmartYamlCompletionContributor])
  
  coolExtendSmart(
    CompletionType.SMART,
    scalaExtendsValueCompletionProvider,
    _
      .withLanguage(YAMLLanguage.INSTANCE)
      .inVirtualFile(
        PlatformPatterns.virtualFile().withName(
          StandardPatterns.string().endsWith(".mill.yaml")
        )
      )
  )

  coolExtendSmart(
    CompletionType.SMART,
    yamlExtendsValueCompletionProvider,
    _
      .withLanguage(YAMLLanguage.INSTANCE)
      .inVirtualFile(
        PlatformPatterns.virtualFile().withName(
          StandardPatterns.string().endsWith(".mill.yaml")
        )
      )
  )

  coolExtendSmart(
    CompletionType.SMART,
    scalaExtendsListCompletionProvider,
    _
      .withLanguage(YAMLLanguage.INSTANCE)
      .inVirtualFile(
        PlatformPatterns.virtualFile().withName(
          StandardPatterns.string().endsWith(".mill.yaml")
        )
      )
  )

  coolExtendSmart(
    CompletionType.SMART,
    yamlExtendsListCompletionProvider,
    _
      .withLanguage(YAMLLanguage.INSTANCE)
      .inVirtualFile(
        PlatformPatterns.virtualFile().withName(
          StandardPatterns.string().endsWith(".mill.yaml")
        )
      )
  )

  coolExtendSmart(
    CompletionType.SMART,
    memberCompletionProvider(logger),
    _
      .withLanguage(YAMLLanguage.INSTANCE)
      .inVirtualFile(
        PlatformPatterns.virtualFile().withName(
          StandardPatterns.string().endsWith(".mill.yaml")
        )
      )
  )

  patternExtendSmart(
    CompletionType.SMART,
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
end SmartYamlCompletionContributor
