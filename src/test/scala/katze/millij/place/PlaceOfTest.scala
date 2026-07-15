package katze.millij.place

import cats.syntax.all.*
import com.intellij.ide.highlighter.XmlFileType
import com.intellij.psi.xml.XmlFile
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.UsefulTestCase.assertInstanceOf
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.PsiErrorElementUtil
import katze.millij.place.TestPlace
import katze.millij.place.TestPlace.{ClassFromName, MemberOf}
import katze.millij.place.placeOf
import katze.millij.psi.CompletionPosition
import org.jetbrains.yaml.psi.{YAMLFile, YAMLMapping, YAMLScalar, YAMLSequence}
import org.junit.Assert.*

import scala.jdk.CollectionConverters.*

@TestDataPath("$CONTENT_ROOT/src/test/testData")
class PlaceOfTest extends BasePlatformTestCase:
  def testKvKeyPlace(): Unit =
    assertTestTypeAt(
      myFixture,
      """
        |extends: [mill.scalalib.ScalaModule, mill.javalib.PublishModule]
        |IntellijIdeaRulezzz<caret>: things
        |some: things
        |
      """.stripMargin,

      Right(
        TestPlace.ClassFromName(List("mill.scalalib.ScalaModule", "mill.javalib.PublishModule"))
      )
    )
  end testKvKeyPlace

  def testTeggedKvKeyPlace(): Unit =
    assertTestTypeAt(
      myFixture,
      """
        |extends: [mill.scalalib.ScalaModule, mill.javalib.PublishModule]
        |seq: !append [IntellijIdeaRulezzz<caret>]
        |some: things
        |
      """.stripMargin,

      Right(
        TestPlace.UnSeqOf(
          MemberOf(
            TestPlace.ClassFromName(List("mill.scalalib.ScalaModule", "mill.javalib.PublishModule")),
            "seq"
          )
        )
      )
    )
  end testTeggedKvKeyPlace
  
  def testGlobalScopeSuggestions(): Unit =
    assertTestTypeAt(
      myFixture,
      """
        |extends: [mill.scalalib.ScalaModule, mill.javalib.PublishModule]
        |IntellijIdeaRulezzz<caret>
        |
      """.stripMargin,
      Right(TestPlace.ClassFromName(List("mill.scalalib.ScalaModule", "mill.javalib.PublishModule")))
    )
    assertTestTypeAt(
      myFixture,
      """
        |extends: [mill.scalalib.ScalaModule, mill.javalib.PublishModule]
        |someIntellijIdeaRulezzz<caret>
        |
      """.stripMargin,
      Right(TestPlace.ClassFromName(List("mill.scalalib.ScalaModule", "mill.javalib.PublishModule")))
    )
    assertTestTypeAt(
      myFixture,
      """
        |extends: [mill.scalalib.ScalaModule, mill.javalib.PublishModule]
        |some: things
        |IntellijIdeaRulezzz<caret>
        |
      """.stripMargin,
      Right(
        TestPlace.ClassFromName(List("mill.scalalib.ScalaModule", "mill.javalib.PublishModule"))
      )
    )
    assertTestTypeAt(
      myFixture,
      """
        |extends: [mill.scalalib.ScalaModule, mill.javalib.PublishModule]
        |IntellijIdeaRulezzz<caret>
        |some: things
        |
      """.stripMargin,

      Right(
        TestPlace.ClassFromName(List("mill.scalalib.ScalaModule", "mill.javalib.PublishModule"))
      )
    )
    assertTestTypeAt(
      myFixture,
      """
        |extends: [mill.scalalib.ScalaModule, mill.javalib.PublishModule]
        |IntellijIdeaRulezzz<caret>: things
        |
      """.stripMargin,

      Right(
        TestPlace.ClassFromName(List("mill.scalalib.ScalaModule", "mill.javalib.PublishModule"))
      )
    )
  end testGlobalScopeSuggestions

  def testFieldSuggestions() : Unit =
    assertTestTypeAt(
      myFixture,
      """
        |extends: [mill.scalalib.ScalaModule, mill.javalib.PublishModule]
        |some: IntellijIdeaRulezzz<caret>
        |
      """.stripMargin,
      Right(
        TestPlace.MemberOf(
          TestPlace.ClassFromName(List("mill.scalalib.ScalaModule", "mill.javalib.PublishModule")),
          "some"
        )
      )
    )
    assertTestTypeAt(
      myFixture,
      """
        |extends: [mill.scalalib.ScalaModule, mill.javalib.PublishModule]
        |some: textIntellijIdeaRulezzz<caret>
        |
      """.stripMargin,
      Right(
        TestPlace.MemberOf(
          TestPlace.ClassFromName(List("mill.scalalib.ScalaModule", "mill.javalib.PublishModule")),
          "some"
        )
      )
    )
    assertTestTypeAt(
      myFixture,
      """
        |extends: [mill.scalalib.ScalaModule, mill.javalib.PublishModule]
        |some:
        | IntellijIdeaRulezzz<caret>
        |
      """.stripMargin,
      Right(
        TestPlace.MemberOf(
          TestPlace.ClassFromName(List("mill.scalalib.ScalaModule", "mill.javalib.PublishModule")),
          "some"
        )
      )
    )
    assertTestTypeAt(
      myFixture,
      """
        |extends: [mill.scalalib.ScalaModule, mill.javalib.PublishModule]
        |some:
        | thingIntellijIdeaRulezzz<caret>
        |
      """.stripMargin,
      Right(
        TestPlace.MemberOf(
          TestPlace.ClassFromName(List("mill.scalalib.ScalaModule", "mill.javalib.PublishModule")),
          "some"
        )
      )
    )
  end testFieldSuggestions

  def testSubObject() : Unit =
    assertTestTypeAt(
      myFixture,
      """
        |extends: [mill.scalalib.ScalaModule, mill.javalib.PublishModule]
        |object test:
        |   some: thingIntellijIdeaRulezzz<caret>
        |
        |
      """.stripMargin,
      Right(
        TestPlace.MemberOf(
          TestPlace.ClassFromName(List()),
          "some"
        )
      )
    )
  end testSubObject
  
  def testField() : Unit =
    assertTestTypeAt(
      myFixture = myFixture, 
      text =  """
                 |extends: ScalaModule
                 |
                 |structMethods:
                 |  a<caret>
                 |""".stripMargin,
      expectedElement = Right(
        MemberOf(
          ClassFromName(List("ScalaModule")),
          "structMethods"
        )
      )
    )
  
  override def getTestDataPath: String = "src/test/testData/rename"
end PlaceOfTest
