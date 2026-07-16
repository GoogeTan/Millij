package katze.millij.annotator

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import katze.millij.MillProjectDescriptor
import org.junit.Assert.*
import scala.jdk.CollectionConverters.*

//TODO add cases where build.A.Trait is overridden
class MillYamlAnnotatorTest extends BasePlatformTestCase:
  override def getProjectDescriptor: LightProjectDescriptor = MillProjectDescriptor

  override def getTestDataPath: String = "src/test/testData"

  override def setUp(): Unit =
    super.setUp()
    // Setup trait/module definitions for smart annotations
    myFixture.copyDirectoryToProject("scalaCode", "src")
  end setUp

  def testObjectInInappropriatePlace(): Unit =
    // 1. Inappropriate place: nested within a non-object key
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |extends: [mill.scalalib.ScalaModule]
        |
        |object myModule:
        |  scalaVersion: "3.3.3"
        |  someField:
        |    object nestedModule:
        |      scalaVersion: "3.3.3"
        |""".stripMargin
    )
    val highlights = myFixture.doHighlighting().asScala
    val errors = highlights.filter(_.getSeverity == HighlightSeverity.ERROR)
    
    // There should be at least one error highlighting on the inappropriately placed object
    val errorOpt = errors.find(_.getText.contains("object nestedModule"))
    assertTrue("Should flag inappropriate nested module declaration", errorOpt.isDefined)
    assertTrue(
      "Error tooltip should describe inappropriate module declaration",
      errorOpt.get.getToolTip.contains("Module declaration is not allowed inside of object instantiation")
    )
  end testObjectInInappropriatePlace

  def testObjectInAppropriatePlace(): Unit =
    // 2. Appropriate place: nested directly inside an object declaration or at document root
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |extends: [mill.scalalib.ScalaModule]
        |
        |object myModule:
        |  scalaVersion: "3.3.3"
        |  object nestedModule:
        |    scalaVersion: "3.3.3"
        |""".stripMargin
    )
    val highlights = myFixture.doHighlighting().asScala
    val errors = highlights.filter(_.getSeverity == HighlightSeverity.ERROR)
    
    // There should be no error flagging nestedModule
    val nestedError = errors.find(_.getText.contains("object nestedModule"))
    assertFalse("Should not flag nested module declaration under another object declaration", nestedError.isDefined)
  end testObjectInAppropriatePlace

  def testUnexistingMembersAnnotator(): Unit =
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |extends: [mill.scalalib.ScalaModule]
        |scalaVersion: "3.3.3"
        |unexistingMember: "someValue"
        |""".stripMargin
    )
    val highlights = myFixture.doHighlighting().asScala
    val errors = highlights.filter(_.getSeverity == HighlightSeverity.ERROR)
    
    // unexistingMember should be flagged, while scalaVersion should not
    val unexistingError = errors.find(_.getText.contains("unexistingMember"))
    val validMemberError = errors.find(_.getText.contains("scalaVersion"))
    
    assertTrue("Should flag unexistingMember", unexistingError.isDefined)
    assertFalse("Should not flag valid member scalaVersion", validMemberError.isDefined)
  end testUnexistingMembersAnnotator

  def testHighlighting(): Unit =
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |extends: [mill.scalalib.ScalaModule]
        |
        |<info descr="null" textAttributesKey="MILL_YAML_OBJECT_KEYWORD">object</info><info descr="null" textAttributesKey="MILL_YAML_MODULE_NAME"> myModule</info>:
        |  extends: [mill.scalalib.ScalaModule]
        |
        |<info descr="null" textAttributesKey="MILL_YAML_MODULE_MEMBER">scalaVersion</info>: "3.3.3"
        |<info descr="null" textAttributesKey="MILL_YAML_MODULE_MEMBER">mvnDeps</info>:
        |  - <info descr="null" textAttributesKey="MILL_MAVEN_DEPENDENCY">"org.scala-lang:scala-library:2.13.12"</info>
        |  - <info descr="null" textAttributesKey="MILL_MAVEN_DEPENDENCY">org.scala-lang:scala-library:2.13.12</info>
        |  - <info descr="null" textAttributesKey="MILL_MAVEN_DEPENDENCY">com.lihaoyi::utest::0.8.9</info>
        |  - <info descr="null" textAttributesKey="MILL_MAVEN_DEPENDENCY">com.lihaoyi::utest::</info>
        |  - <info descr="null" textAttributesKey="MILL_MAVEN_DEPENDENCY">com.lihaoyi::utest:</info>
        |  - <info descr="null" textAttributesKey="MILL_MAVEN_DEPENDENCY">com.lihaoyi:utest:</info>
        |  - <info descr="null" textAttributesKey="MILL_MAVEN_DEPENDENCY">com.lihaoyi::ut</info>
        |  - <info descr="null" textAttributesKey="MILL_MAVEN_DEPENDENCY">com.lihaoyi:u</info>
        |  - <info descr="null" textAttributesKey="MILL_MAVEN_DEPENDENCY">com.lihaoyi:</info>
        |  - <info descr="null" textAttributesKey="MILL_MAVEN_DEPENDENCY">com.lihao</info>
        |""".stripMargin
    )
    myFixture.checkHighlighting(false, true, false)
  end testHighlighting
end MillYamlAnnotatorTest
