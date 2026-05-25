package katze.millij.completions

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.openapi.editor.Editor
import com.intellij.psi.{PsiElement, PsiFile}
import com.intellij.util.ThreeState
import org.jetbrains.plugins.scala.lang.psi.api.base.ScInterpolatedStringLiteral

def isMvnDependencyInterpolatedString(sc : ScInterpolatedStringLiteral) : Boolean =
  sc.referenceName == "mvn" || sc.referenceName == "ivy"
end isMvnDependencyInterpolatedString