package katze.millij.reposearch

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.{PlatformPatterns, StandardPatterns}
import katze.millij.completions.cool.SmartCoolCompletionContributor
import katze.millij.cool.CoolPattern
import katze.millij.psi.CompletionPosition
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.YAMLPsiElement

final class YamlMavenCompletionContributor extends SmartCoolCompletionContributor:
  patternExtendSmart(
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
end YamlMavenCompletionContributor