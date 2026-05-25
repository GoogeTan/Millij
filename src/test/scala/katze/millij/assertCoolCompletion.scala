package katze.millij

import com.intellij.psi.PsiElement
import com.intellij.testFramework.UsefulTestCase.assertInstanceOf
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import katze.millij.completions.cool.CoolCompletionProvider
import katze.millij.cool.{PsiElementMatcher, PsiParent}
import org.jetbrains.yaml.psi.YAMLFile
import org.junit.Assert.assertTrue

def assertCoolCompletionMatch[
  Element <: PsiElement : PsiElementMatcher as pem,
  Parents : PsiParent as psiParents
](
  myFixture: CodeInsightTestFixture,
  text: String,
  provider: CoolCompletionProvider[Element, Parents]
): Unit =
  val psiFile = myFixture.configureByText("build.mill.yaml", text)
  assertInstanceOf(psiFile, classOf[YAMLFile])
  val offset = myFixture.getCaretOffset

  var elementAtCaret = psiFile.findElementAt(offset)

  if (elementAtCaret != null && elementAtCaret.getTextRange.getStartOffset == offset) {
    elementAtCaret = psiFile.findElementAt(Math.max(0, offset - 1))
  }

  val ext = pem.extract(elementAtCaret)
  assertTrue(s"Failed to extract element with PsiElementMatcher from $elementAtCaret", ext.isDefined)
  val element = ext.get

  val parent = element.getParent

  val parentsExt = psiParents.test(parent)
  assertTrue(s"Failed to match parents with PsiParent from $parent", parentsExt.isDefined)
  val parents = parentsExt.get

  // We are not executing completion here since it requires fully mocked CompletionParameters,
  // but we are asserting that the PsiElement matching matches perfectly.
  // Full integration completion tests can be done directly via myFixture.completeBasic().
end assertCoolCompletionMatch
