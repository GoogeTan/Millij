package katze.millij

import cats.syntax.all.*
import com.intellij.testFramework.UsefulTestCase.assertInstanceOf
import com.intellij.testFramework.fixtures.CodeInsightTestFixture
import katze.millij.psi.CompletionPosition
import org.jetbrains.yaml.psi.YAMLFile
import org.junit.Assert.assertEquals

def assertAutocomplete(myFixture : CodeInsightTestFixture, text : String, expectedElement : Either[String, TestType]) : Unit =
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
  val scope = testScopeOf(caretElement).leftMap(_.mkString_("\n"))
  assertEquals(expectedElement, scope)
end assertAutocomplete


def assertMatches[A, B](value: A, matcher: A => Option[B], text: String = "Failed to match"): B =
  matcher(value) match
    case Some(value) =>
      value
    case None =>
      assert(false, text)
      ???
end assertMatches
