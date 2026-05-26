package katze.millij.completions.cool

import com.intellij.codeInsight.completion.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.patterns.{ElementPattern, PlatformPatterns, PsiElementPattern}
import com.intellij.psi.PsiElement
import com.intellij.util.ProcessingContext
import katze.millij.cool.{CoolPattern, PsiElementMatcher, PsiParentElementMatcher}

import scala.reflect.ClassTag

/**
 * Adds [[CoolPattern]] based methods to [[CompletionContributor]]
 */
trait CoolCompletionContributor extends CompletionContributor:
  private val LOG = Logger.getInstance(classOf[CoolCompletionContributor])

  def patternExtend[
    Data
  ](
    completionType : CompletionType,
    pattern : CoolPattern[Data],
    place : PsiElementPattern.Capture[? <: PsiElement] => ElementPattern[? <: PsiElement]
  )(
    completionProvider : (Data, ProcessingContext, CompletionResultSet) => Unit
  ) : Unit =
    extend(
      completionType,
      place(PlatformPatterns.psiElement().and(pattern.pattern)),
      new CompletionProvider[CompletionParameters]:
        override def addCompletions(v: CompletionParameters, processingContext: ProcessingContext, completionResultSet: CompletionResultSet): Unit =
          val position = v.getPosition
          if !position.isValid then
            return
          pattern.extract(position) match
            case Some(value) =>
              completionProvider(value, processingContext, completionResultSet)
            case None =>
              println("Pattern failed!")
    )
  end patternExtend
      
  def coolExtend[
    Element <: PsiElement : PsiElementMatcher as pem,
    Parents  : PsiParentElementMatcher as psiParents
  ](
    `type`: CompletionType,
    provider: CoolCompletionProvider[Element, Parents],
    place: PsiElementPattern.Capture[Element] => ElementPattern[? <: PsiElement]
  ): Unit =
    extend(
      `type`,
      place(psiParents.appendTo(pem.capture)),
      new CompletionProvider[CompletionParameters]:
        override def addCompletions(
          parameters: CompletionParameters,
          context: ProcessingContext,
          result: CompletionResultSet
        ): Unit =
          val position = parameters.getPosition
          if !position.isValid then
            return
          end if

          (pem.extract(position), psiParents.test(position.getParent)) match
            case (Some(element), Some(parents)) =>
              provider(parameters, element, parents, context, result)
            case _ =>
              LOG.error(s"Encountered impossible situation. Please, report to to mill suggestion")
        end addCompletions
    )
end CoolCompletionContributor