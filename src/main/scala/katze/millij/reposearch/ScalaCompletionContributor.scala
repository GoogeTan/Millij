package katze.millij.reposearch

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.patterns.StandardPatterns.or
import com.intellij.patterns.{PlatformPatterns, StandardPatterns}
import katze.millij.completions.cool.CoolCompletionContributor
import katze.millij.scalatypes.isMavenDependencyInterpolatedStringLiteral
import katze.millij.psi.CompletionPosition
import katze.millij.reposearch.ScalaMavenDependenciesCompletionProvider
import org.jetbrains.plugins.scala.lang.psi.api.base.ScInterpolatedStringLiteral
import org.jetbrains.plugins.scala.{Scala3Language, ScalaLanguage}

final class ScalaCompletionContributor extends CoolCompletionContributor:
  extend(
    CompletionType.BASIC,
    PlatformPatterns
      .psiElement()
      .withLanguage(Scala3Language.INSTANCE)
      .inVirtualFile(
        PlatformPatterns
          .virtualFile()
          .withName(
            StandardPatterns.or(
              StandardPatterns.string().endsWith(".mill"),
              StandardPatterns.string().endsWith(".sc"),
            )
          )
      ),
    (params, context, result) =>
      val current = params.getPosition.asInstanceOf[CompletionPosition]
      current.getParent match
        case sc : ScInterpolatedStringLiteral if isMavenDependencyInterpolatedStringLiteral(sc) =>
          ScalaMavenDependenciesCompletionProvider.addCompletions(params, context, result)
        case _ =>
          ()
  )
end ScalaCompletionContributor
