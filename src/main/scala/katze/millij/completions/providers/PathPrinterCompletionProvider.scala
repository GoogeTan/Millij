package katze.millij.completions.providers

import com.intellij.codeInsight.completion.{CompletionParameters, CompletionProvider, CompletionResultSet}
import com.intellij.util.ProcessingContext
import katze.millij.psi.PsiChild

object PathPrinterCompletionProvider extends CompletionProvider[CompletionParameters]:
  override def addCompletions(v: CompletionParameters, processingContext: ProcessingContext, completionResultSet: CompletionResultSet): Unit =
    println(s"Input position: ${PsiChild.unapplySeq(v.getPosition).map(_.toString).mkString(" -> ")}")
  end addCompletions
end PathPrinterCompletionProvider

