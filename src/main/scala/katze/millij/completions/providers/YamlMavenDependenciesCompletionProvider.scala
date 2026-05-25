package katze.millij.completions.providers

import com.intellij.codeInsight.completion.*
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import katze.millij.*
import katze.millij.completions.cool.CoolCompletionProvider
import katze.millij.cool.CoolPattern
import katze.millij.data.MavenDependencyShared
import katze.millij.psi.PsiChild
import katze.millij.psi.CompletionPosition
import org.jetbrains.yaml.psi.*

val yamlMavenDependenciesPattern =
  (
  CoolPattern.elementAndParents[CompletionPosition, (YAMLScalar, YAMLSequenceItem)]()
      .map ((_, scalar, _) => scalar.getText)
    || CoolPattern.elementAndParents[CompletionPosition, (YAMLScalar, YAMLKeyValue, YAMLSequenceItem)]()
      .map((_, scalar, kv, _) => kv.getText)
    || CoolPattern.elementAndParents[CompletionPosition, (YAMLKeyValue, YAMLSequenceItem)]()
      .map((_, kv, _) => kv.getText)
  ).map(cleanElementTextFromDummyIdentifier)
end yamlMavenDependenciesPattern

def cleanElementTextFromDummyIdentifier(text: String): String =
  text.replace(CompletionUtilCore.DUMMY_IDENTIFIER, "")
    .replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "")
end cleanElementTextFromDummyIdentifier

def suggestMavenDependency(element: CompletionPosition, dependencyText: String, resultSet: CompletionResultSet): Unit =
  richScopeOf(element).foreach:
    case PlaceInYamlConfig.Member(_, _, expectedType, _) if expectedType.canonicalText.endsWith("Dep") =>
      MavenDependencyShared.searchAndSuggestDependencies(resultSet, "3.0.0", element.getProject, "com.google.code.gson:gs")
    case _ =>
end suggestMavenDependency