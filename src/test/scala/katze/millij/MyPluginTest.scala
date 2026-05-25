package katze.millij

import cats.syntax.all.*
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.UsefulTestCase.assertInstanceOf
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.PsiErrorElementUtil
import katze.millij.scopeOf
import katze.millij.psi.CompletionPosition
import org.jetbrains.yaml.psi.{YAMLFile, YAMLMapping, YAMLScalar, YAMLSequence}
import org.junit.Assert.*

import scala.jdk.CollectionConverters.*

@TestDataPath("$CONTENT_ROOT/src/test/testData")
class MyPluginTest extends BasePlatformTestCase:
  def testGlobalScopeSuggestions(): Unit =
    assertAutocomplete(
      myFixture,
      """
        |extends: [mill.scalalib.ScalaModule, mill.javalib.PublishModule]
        |IntellijIdeaRulezzz<caret>
        |
      """.stripMargin,
      Right(TestType.ClassFromName(List("mill.scalalib.ScalaModule", "mill.javalib.PublishModule")))
    )
    assertAutocomplete(
      myFixture,
      """
        |extends: [mill.scalalib.ScalaModule, mill.javalib.PublishModule]
        |someIntellijIdeaRulezzz<caret>
        |
      """.stripMargin,
      Right(TestType.ClassFromName(List("mill.scalalib.ScalaModule", "mill.javalib.PublishModule")))
    )
    assertAutocomplete(
      myFixture,
      """
        |extends: [mill.scalalib.ScalaModule, mill.javalib.PublishModule]
        |some: things
        |IntellijIdeaRulezzz<caret>
        |
      """.stripMargin,
      Right(
        TestType.ClassFromName(List("mill.scalalib.ScalaModule", "mill.javalib.PublishModule"))
      )
    )
    assertAutocomplete(
      myFixture,
      """
        |extends: [mill.scalalib.ScalaModule, mill.javalib.PublishModule]
        |IntellijIdeaRulezzz<caret>
        |some: things
        |
      """.stripMargin,

      Right(
        TestType.ClassFromName(List("mill.scalalib.ScalaModule", "mill.javalib.PublishModule"))
      )
    )
    assertAutocomplete(
      myFixture,
      """
        |extends: [mill.scalalib.ScalaModule, mill.javalib.PublishModule]
        |IntellijIdeaRulezzz<caret>: things
        |some: things
        |
      """.stripMargin,

      Right(
        TestType.ClassFromName(List("mill.scalalib.ScalaModule", "mill.javalib.PublishModule"))
      )
    )
    assertAutocomplete(
      myFixture,
      """
        |extends: [mill.scalalib.ScalaModule, mill.javalib.PublishModule]
        |IntellijIdeaRulezzz<caret>: things
        |
      """.stripMargin,

      Right(
        TestType.ClassFromName(List("mill.scalalib.ScalaModule", "mill.javalib.PublishModule"))
      )
    )
  end testGlobalScopeSuggestions

  def testFieldSuggestions() : Unit =
    assertAutocomplete(
      myFixture,
      """
        |extends: [mill.scalalib.ScalaModule, mill.javalib.PublishModule]
        |some: IntellijIdeaRulezzz<caret>
        |
      """.stripMargin,
      Right(
        TestType.MemberOf(
          TestType.ClassFromName(List("mill.scalalib.ScalaModule", "mill.javalib.PublishModule")),
          "some"
        )
      )
    )
    assertAutocomplete(
      myFixture,
      """
        |extends: [mill.scalalib.ScalaModule, mill.javalib.PublishModule]
        |some: textIntellijIdeaRulezzz<caret>
        |
      """.stripMargin,
      Right(
        TestType.MemberOf(
          TestType.ClassFromName(List("mill.scalalib.ScalaModule", "mill.javalib.PublishModule")),
          "some"
        )
      )
    )
    assertAutocomplete(
      myFixture,
      """
        |extends: [mill.scalalib.ScalaModule, mill.javalib.PublishModule]
        |some:
        | IntellijIdeaRulezzz<caret>
        |
      """.stripMargin,
      Right(
        TestType.MemberOf(
          TestType.ClassFromName(List("mill.scalalib.ScalaModule", "mill.javalib.PublishModule")),
          "some"
        )
      )
    )
    assertAutocomplete(
      myFixture,
      """
        |extends: [mill.scalalib.ScalaModule, mill.javalib.PublishModule]
        |some:
        | thingIntellijIdeaRulezzz<caret>
        |
      """.stripMargin,
      Right(
        TestType.MemberOf(
          TestType.ClassFromName(List("mill.scalalib.ScalaModule", "mill.javalib.PublishModule")),
          "some"
        )
      )
    )
  end testFieldSuggestions

  def testSubObject() : Unit =
    assertAutocomplete(
      myFixture,
      """
        |extends: [mill.scalalib.ScalaModule, mill.javalib.PublishModule]
        |object test:
        |   some: thingIntellijIdeaRulezzz<caret>
        |
        |
      """.stripMargin,
      Right(
        TestType.MemberOf(
          TestType.ClassFromName(List()),
          "some"
        )
      )
    )
  end testSubObject
  override def getTestDataPath: String = "src/test/testData/rename"
end MyPluginTest
