package katze.millij.reposearch

import com.intellij.codeInsight.completion.*
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import katze.millij.data.MavenDependencyShared
import org.jetbrains.plugins.scala.lang.psi.api.base.ScInterpolatedStringLiteral

import scala.util.matching.Regex

/**
 * Adds maven dependencies suggestions for mvn"" interpolator
 * @see [[katze.millij.completions.providers.suggestMavenDependency]] for YAML conterpart
 */
object ScalaMavenDependenciesCompletionProvider extends CompletionProvider[CompletionParameters]:
  override def addCompletions(
    v: CompletionParameters,
    processingContext: ProcessingContext,
    completionResultSet: CompletionResultSet
  ): Unit =
    val psiElement = v.getPosition
    val fullText = psiElement.getText
      .replace(CompletionUtilCore.DUMMY_IDENTIFIER, "")
      .replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "")
      .trim
    MavenDependencyShared.searchAndSuggestDependencies(completionResultSet, "3.3.3", psiElement.getProject, fullText)
  end addCompletions
end ScalaMavenDependenciesCompletionProvider