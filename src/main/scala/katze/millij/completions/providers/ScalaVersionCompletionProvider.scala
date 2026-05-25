package katze.millij.completions.providers

import com.intellij.codeInsight.completion.{CompletionParameters, CompletionResultSet, PrioritizedLookupElement}
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.util.ProcessingContext
import com.intellij.util.text.VersionComparatorUtil
import katze.millij.completions.cool.*
import katze.millij.psi.YAMLMillModule
import katze.millij.data.ScalaVersionCacheService
import katze.millij.psi.*
import org.jetbrains.yaml.psi.{YAMLKeyValue, YAMLMapping, YAMLScalar}

def scalaVersionCompletionProvider : CoolCompletionProvider[
  CompletionPosition,
  YAMLScalar *: YAMLKeyValue *: YAMLMapping *: YAMLMillModule *: EmptyTuple
] =
  case (
    parameters: CompletionParameters,
    element: CompletionPosition,
    scalar *: field *: _ *: _ *: EmptyTuple,
    context : ProcessingContext,
    resultSet: CompletionResultSet
  ) =>
    if scalar == field.getValue && field.getKeyText == "scalaVersion" then
      ScalaVersionCacheService
        .getInstance
        .getVersions
        .sortWith((a, b) => VersionComparatorUtil.compare(a, b) < 0)
        .zipWithIndex
        .foreach((version, index) =>
          resultSet.addElement(
            PrioritizedLookupElement.withPriority(
              LookupElementBuilder
                .create(version)
                .withTypeText("ScalaVersion"),
              index
            )
          )
        )
    end if
end scalaVersionCompletionProvider
