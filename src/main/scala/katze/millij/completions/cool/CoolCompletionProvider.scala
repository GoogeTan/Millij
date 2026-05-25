package katze.millij.completions.cool

import com.intellij.codeInsight.completion.{CompletionParameters, CompletionResultSet}
import com.intellij.util.ProcessingContext

/**
 * Provides completion items.
 * <p>
 * Register via {@link CoolCompletionContributor#extend(CompletionType, ElementPattern, CompletionProvider)}.
 */
type CoolCompletionProvider[Element, Parents] = (CompletionParameters, Element, Parents, ProcessingContext, CompletionResultSet) => Unit

