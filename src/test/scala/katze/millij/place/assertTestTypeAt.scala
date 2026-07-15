package katze.millij.place

import cats.syntax.all.*
import com.intellij.testFramework.UsefulTestCase.assertInstanceOf
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import katze.millij.psi.CompletionPosition
import katze.millij.*
import org.jetbrains.yaml.psi.{YAMLFile, YAMLPsiElement}
import org.junit.Assert.assertEquals

/**
 * Asserts that text type under cursor is equals to the expected one
 * @param text document text with a caret present
 * @param expectedElement expected test type at caret position
 */
def assertTestTypeAt(myFixture : CodeInsightTestFixture, text : String, expectedElement : Either[String, TestPlace]) : Unit =
  val psiFile = myFixture.configureByText("build.mill.yaml", text)
  val yamlFile = assertInstanceOf(psiFile, classOf[YAMLFile])
  val offset = myFixture.getCaretOffset
  
  var elementAtCaret = psiFile.findElementAt(offset)

  // Caret Boundary Check:
  // If the caret offset is exactly at the start of the returned element,
  // we are at a boundary (e.g., hitting the EOL or next token). Step back by 1.
  if elementAtCaret != null && elementAtCaret.getTextRange.getStartOffset == offset then
    elementAtCaret = psiFile.findElementAt(Math.max(0, offset - 1))
  end if

  val caretElement : CompletionPosition = assertMatches(elementAtCaret, CompletionPosition.unapply)
  val scope = testPlaceOf(caretElement).leftMap(_.mkString_("\n"))
  assertEquals(expectedElement, scope)
end assertTestTypeAt