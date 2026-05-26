package katze.millij.completions

import com.intellij.codeInsight.completion.CompletionConfidence
import com.intellij.openapi.editor.Editor
import com.intellij.psi.{PsiElement, PsiFile}
import com.intellij.util.ThreeState
import org.jetbrains.plugins.scala.lang.psi.api.base.ScInterpolatedStringLiteral

/**
 * Allows completions inside maven dependency interpolated string literal
 */
class MillStringCompletionConfidence extends CompletionConfidence:
  override def shouldSkipAutopopup(contextElement: PsiElement, psiFile: PsiFile, offset: Int): ThreeState =
    handleConfidence(contextElement, psiFile)

  override def shouldSkipAutopopup(editor: Editor, contextElement: PsiElement, psiFile: PsiFile, offset: Int): ThreeState =
    handleConfidence(contextElement, psiFile)

  private def handleConfidence(contextElement: PsiElement, psiFile: PsiFile): ThreeState =//TODO remove logs
    println("1")
    if !psiFile.getName.endsWith(".mill") && !psiFile.getName.endsWith(".sc") then
      return ThreeState.UNSURE
    println("2")
    contextElement.getParent match
      case stringLiteral: ScInterpolatedStringLiteral if isMavenDependencyInterpolatedStringLiteral(stringLiteral) =>
        ThreeState.NO
      case _ =>
        ThreeState.UNSURE
    end match
  end handleConfidence
end MillStringCompletionConfidence
