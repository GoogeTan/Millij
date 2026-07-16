package katze.millij.completions

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import katze.millij.{MillProjectDescriptor, Scala3ProjectDescriptor}
import org.junit.Assert.{assertArrayEquals, assertEquals, assertFalse, assertNotNull, assertTrue, fail}

class YamlMemberCompletionTest extends BasePlatformTestCase:
  override def getProjectDescriptor: LightProjectDescriptor = MillProjectDescriptor

  override def getTestDataPath: String = "src/test/testData"
  
  override def setUp(): Unit =
    super.setUp()
    myFixture.copyDirectoryToProject("scalaCode", "src")
  end setUp

  def testTopScopeMethodNameCompletion(): Unit =
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |extends: SbtModule
        |
        |<caret>
        |""".stripMargin
    )
    val lookupElements = myFixture.complete(CompletionType.SMART)
    assertNotNull("Lookup elements should not be null", lookupElements)
    lookupElements.find(_.getLookupString == "somethingWithTask") match
      case Some(elementFound) =>
        val presentation = LookupElementPresentation()
        elementFound.renderElement(presentation)
        assertEquals("String", presentation.getTypeText)
      case None =>
        fail("Expected 'somethingWithTask' to be in completion list")
  end testTopScopeMethodNameCompletion

  def testStructCompletion() : Unit =
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |extends: ScalaModule
        |
        |structMethods:
        |  <caret>
        |""".stripMargin
    )
    val lookupElements = myFixture.complete(CompletionType.SMART)
    assertEquals(3, lookupElements.length)
    assertNotNull("Lookup elements should not be null", lookupElements)
    val sortedLookupStrings = lookupElements.map(_.getLookupString).sorted
    assertArrayEquals(
      sortedLookupStrings.map(a => a) : Array[Any],
      Array[Any]("a", "b", "c") : Array[Any]
    )
  end testStructCompletion

  def testStructDefinedMembersFiltering() : Unit =
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |extends: ScalaModule
        |
        |structMethods:
        |  b: ""
        |  <caret>
        |""".stripMargin
    )
    val lookupElements = myFixture.complete(CompletionType.SMART)
    assertEquals(2, lookupElements.length)
    assertNotNull("Lookup elements should not be null", lookupElements)
    val sortedLookupStrings = lookupElements.map(_.getLookupString).sorted
    assertArrayEquals(
      sortedLookupStrings.map(a => a) : Array[Any],
      Array[Any]("a", "c") : Array[Any]
    )
  end testStructDefinedMembersFiltering

  def testSeqValueCompletion(): Unit =
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |extends: ScalaModule
        |
        |seqMethod:
        |  - <caret>
        |""".stripMargin
    )
    val lookupElements = myFixture.complete(CompletionType.SMART)
    assertEquals(3, lookupElements.length)
    assertNotNull("Lookup elements should not be null", lookupElements)
    val sortedLookupStrings = lookupElements.map(_.getLookupString).sorted
    assertArrayEquals(
      sortedLookupStrings.map(a => a) : Array[Any],
      Array[Any]("a", "b", "c") : Array[Any]
    )
  end testSeqValueCompletion


  def testNestedStructCompletion() : Unit =
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |extends: ScalaModule
        |
        |structMethods:
        |  c:
        |     <caret>
        |""".stripMargin
    )
    val lookupElements = myFixture.complete(CompletionType.SMART)
    assertEquals(3, lookupElements.length)
    assertNotNull("Lookup elements should not be null", lookupElements)
    val sortedLookupStrings = lookupElements.map(_.getLookupString).sorted
    assertArrayEquals(
      sortedLookupStrings.map(a => a) : Array[Any],
      Array[Any]("a", "b", "c") : Array[Any]
    )
  end testNestedStructCompletion

  def testDependentPathMemberTypeCompletion() : Unit =
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |extends: [Dep2, Dep4]
        |
        |object inner:
        | extends: Dep3
        | foo:
        |   <caret>
        |""".stripMargin
    )
    val lookupElements = myFixture.complete(CompletionType.SMART)
    assertEquals(3, lookupElements.length)
    assertNotNull("Lookup elements should not be null", lookupElements)
    val sortedLookupStrings = lookupElements.map(_.getLookupString).sorted
    assertArrayEquals(
      sortedLookupStrings.map(a => a) : Array[Any],
      Array[Any]("a", "b", "c") : Array[Any]
    )
  end testDependentPathMemberTypeCompletion
end YamlMemberCompletionTest
