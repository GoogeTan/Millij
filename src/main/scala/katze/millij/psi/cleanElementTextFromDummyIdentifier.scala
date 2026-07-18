package katze.millij.psi

import com.intellij.codeInsight.completion.CompletionUtilCore

def cleanElementTextFromDummyIdentifier(text: String): String =
  text.replace(CompletionUtilCore.DUMMY_IDENTIFIER, "")
    .replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "")
end cleanElementTextFromDummyIdentifier
