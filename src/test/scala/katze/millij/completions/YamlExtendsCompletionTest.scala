package katze.millij.completions

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import katze.millij.{MillProjectDescriptor, Scala3ProjectDescriptor}
import org.junit.Assert.*

class YamlExtendsCompletionTest extends BasePlatformTestCase:
  override def getProjectDescriptor: LightProjectDescriptor = MillProjectDescriptor

  override def getTestDataPath: String = "src/test/testData"

  override def setUp(): Unit =
    super.setUp()
    myFixture.copyDirectoryToProject("scalaCode", "src")
  end setUp

  def testExtendsKeywordCompletion(): Unit =
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |object A:
        |  scalaVersion: "2.13.11"
        |  ext<caret>
        |""".stripMargin
    )
    val lookupElements = myFixture.completeBasic()
    assertNotNull("Lookup elements should not be null", lookupElements)
    val lookupStrings = lookupElements.map(_.getLookupString).toList
    assertTrue("Expected 'extends' to be in completion list", lookupStrings.contains("extends"))
  end testExtendsKeywordCompletion
  
  def testExtendsKeywordInKeyPositionCompletion(): Unit =
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |object A:
        |  scalaVersion: "2.13.11"
        |  ext<caret>: []
        |""".stripMargin
    )
    val lookupElements = myFixture.completeBasic()
    assertNotNull("Lookup elements should not be null", lookupElements)
    val lookupStrings = lookupElements.map(_.getLookupString).toList
    assertTrue("Expected 'extends' to be in completion list", lookupStrings.contains("extends"))
  end testExtendsKeywordInKeyPositionCompletion
  
  def testNoExtendsKeywordCompletionWhenExtendsIsPresent(): Unit =
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |object A:
        |  scalaVersion: "2.13.11"
        |  extends: []
        |  ext<caret>
        |""".stripMargin
    )
    val lookupElements = myFixture.completeBasic()
    assertTrue(
      s"Lookup elements should be null or empty.", 
      lookupElements == null || lookupElements.isEmpty
    )
  end testNoExtendsKeywordCompletionWhenExtendsIsPresent
  
  def testExtendsListCompletion(): Unit =
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |extends: [Sbt<caret>]
        |""".stripMargin
    )
    val lookupElements = myFixture.complete(CompletionType.BASIC)
    assertNotNull("Lookup elements should not be null", lookupElements)
    val lookupStrings = lookupElements.map(_.getLookupString).toList
    assertTrue("Expected 'SbtModule' to be in completion list", lookupStrings.contains("SbtModule"))
    assertTrue("Expected 'SbtModule2' to be in completion list", lookupStrings.contains("SbtModule2"))
  end testExtendsListCompletion

  def testExtendsValueCompletion(): Unit =
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |extends: Sbt<caret>
        |""".stripMargin
    )
    val lookupElements = myFixture.complete(CompletionType.BASIC)
    assertNotNull("Lookup elements should not be null", lookupElements)
    val lookupStrings = lookupElements.map(_.getLookupString).toList
    assertTrue("Expected 'SbtModule' to be in completion list", lookupStrings.contains("SbtModule"))
    assertTrue("Expected 'SbtModule2' to be in completion list", lookupStrings.contains("SbtModule2"))
  end testExtendsValueCompletion


  def testDependentTraitInNestedCompletion(): Unit =
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |extends: SbtModule
        |
        |object A:
        | extends: Sbt<caret>
        |""".stripMargin
    )
    val lookupElements = myFixture.complete(CompletionType.BASIC)
    assertNotNull("Lookup elements should not be null", lookupElements)
    val lookupStrings = lookupElements.map(_.getLookupString).toList
    assertTrue("Expected 'SbtTests' to be in completion list", lookupStrings.contains("SbtTests"))
    assertTrue("Expected 'SbtModule' to be in completion list", lookupStrings.contains("SbtModule"))
    assertTrue("Expected 'SbtModule2' to be in completion list", lookupStrings.contains("SbtModule2"))
  end testDependentTraitInNestedCompletion

  def testExplicitDependentTraitInNestedCompletion(): Unit =
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |extends: SbtModule
        |
        |object A:
        | extends: build.<caret>
        |""".stripMargin
    )
    val lookupElements = myFixture.complete(CompletionType.BASIC)
    assertNotNull("Lookup elements should not be null", lookupElements)
    val lookupStrings = lookupElements.map(_.getLookupString).toList
    assertTrue("Expected 'SbtTests' to be in completion list", lookupStrings.contains("SbtTests"))
  end testExplicitDependentTraitInNestedCompletion

  def testDependentTraitCompletion(): Unit =
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |object A:
        |  extends: SbtModule
        |
        |object B:
        | extends: A.<caret>
        |""".stripMargin
    )
    val lookupElements = myFixture.complete(CompletionType.BASIC)
    assertNotNull("Lookup elements should not be null", lookupElements)
    val lookupStrings = lookupElements.map(_.getLookupString).toList
    assertTrue("Expected 'A.SbtTests' to be in completion list", lookupStrings.contains("A.SbtTests"))
  end testDependentTraitCompletion


  def testExplicitDependentTraitCompletion(): Unit =
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |object A:
        |  extends: SbtModule
        |
        |object B:
        | extends: build.A.<caret>
        |""".stripMargin
    )
    val lookupElements = myFixture.complete(CompletionType.BASIC)
    assertNotNull("Lookup elements should not be null", lookupElements)
    val lookupStrings = lookupElements.map(_.getLookupString).toList
    assertTrue("Expected 'A.SbtTests' to be in completion list", lookupStrings.contains("A.SbtTests"))
  end testExplicitDependentTraitCompletion
end YamlExtendsCompletionTest
