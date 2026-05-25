package katze.millij

import com.intellij.testFramework.fixtures.BasePlatformTestCase
import org.junit.Assert.assertTrue

class PathPrinterCompletionProviderTest extends BasePlatformTestCase:

  def testPrintCursorPathInYaml(): Unit =
    myFixture.configureByText(
      "test.yaml",
      """
        |someKey:
        |  nested:
        |    - item<caret>
        |""".stripMargin
    )

    System.out.println("=== PathPrinter TEST START ===")

    // 1. Get the current caret offset
    val offset = myFixture.getCaretOffset
    var elementAtCaret = myFixture.getFile.findElementAt(offset)
    if elementAtCaret != null && elementAtCaret.getText == "\n" then
      elementAtCaret = myFixture.getFile.findElementAt(offset - 1)

    import katze.millij.psi.PsiChild
    System.out.println(s"Input position: ${PsiChild.unapplySeq(elementAtCaret).map(_.toString).mkString(" -> ")}")

    System.out.println("=== PathPrinter TEST END ===")
    assertTrue(true)
  end testPrintCursorPathInYaml
end PathPrinterCompletionProviderTest