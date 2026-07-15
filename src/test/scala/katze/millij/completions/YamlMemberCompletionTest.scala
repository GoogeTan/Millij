package katze.millij.completions

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElementPresentation
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import katze.millij.{MillProjectDescriptor, Scala3ProjectDescriptor}
import org.junit.Assert.{assertArrayEquals, assertEquals, assertFalse, assertNotNull, assertTrue, fail}

class YamlMemberCompletionTest extends BasePlatformTestCase:
  override def getProjectDescriptor: LightProjectDescriptor = MillProjectDescriptor
  
  override def setUp(): Unit =
    super.setUp()
    myFixture.addFileToProject(
      "src/mill/api/Task.scala",
      """
        |package mill.api
        |
        |sealed trait Task[+T]
        |""".stripMargin
    )
    myFixture.addFileToProject(
      "src/mill/api/TestStruct.scala",
      """
        |package mill.api
        |
        |final case class TestStruct(val a : String, val b : String, val c : String)
        |final case class NestedStruct(val a : String, val b : String, val c : TestStruct)
        |""".stripMargin
    )
    myFixture.addFileToProject(
      "src/mill/api/Module.scala",
      """package mill.api
        |
        |trait Module
        |""".stripMargin
    )
    myFixture.addFileToProject(
      "src/mill/scalalib/ScalaModule.scala",
      """package mill.scalalib
        |
        |trait ScalaModule extends mill.api.Module {
        |  def scalaVersion : String
        |
        |  def seqMethod : Seq[mill.api.TestStruct]
        |
        |  def structMethods : mill.api.NestedStruct
        |}
        |""".stripMargin
    )
    myFixture.addFileToProject(
      "src/mill/scalalib/SbtModule.scala",
      """package mill.scalalib
        |
        |import mill.api._
        |
        |trait SbtModule extends ScalaModule {
        |  trait SbtTests {
        |     def something : String
        |  }
        |  def somethingWithTask : Task[String] 
        |}
        |""".stripMargin
    )
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
end YamlMemberCompletionTest
