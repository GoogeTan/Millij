package katze.millij.completions.providers

import com.intellij.codeInsight.completion.*
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import katze.millij.*
import katze.millij.annotator.isMvnDependency
import katze.millij.completions.cool.CoolCompletionProvider
import katze.millij.cool.CoolPattern
import katze.millij.data.{MavenDependencyShared, Smart}
import katze.millij.place.*
import katze.millij.psi.{CompletionPosition, PsiChild}
import org.jetbrains.yaml.psi.*

/**
 * A pattern that matches maven dependency being input in yaml document.
 * The result is the text of the dependency as it is written in the document.
 *
 * It is that complex because yaml is not that intended for things like "org.typelevel::cats-core:x.y.z". It is interpreted
 * as a scalar at the begining:
 * ```YAML
 * org.type<caret>
 * ```
 * then it is treated as key value:
 * ```YAML
 * org.typelevel:<caret>
 * ```
 * then it is treated as a scalar body of key value: //TODO check this fact for :: but it is definitely true for : chains.
 * ```YAML
 * org.typelevel::cats-core<caret>
 * ```
 */
val yamlMavenDependenciesPattern : CoolPattern[String] =
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

/**
 * Adds maven dependencies suggestions for YAML.
 * @param element element to search scope
 * @param dependencyText The cleaned text of the dependency being typed
 * @param resultSet
 * @see [[ScalaMavenDependenciesCompletionProvider]] for scala conterpart
 */
def suggestMavenDependency(element: YAMLPsiElement, dependencyText: String, resultSet: CompletionResultSet)(using Smart): Unit =
  richPlaceOf(element).foreach:
    case PlaceInYamlConfig.Member(_, expectedType, _) if isMvnDependency(expectedType) =>
      MavenDependencyShared.searchAndSuggestDependencies(resultSet, "3.0.0", element.getProject, dependencyText)
    case _ =>
end suggestMavenDependency