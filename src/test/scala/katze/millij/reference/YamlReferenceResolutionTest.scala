package katze.millij.reference

import cats.syntax.all.*
import com.intellij.psi.{PsiElement, PsiPolyVariantReference}
import com.intellij.testFramework.LightProjectDescriptor
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import katze.millij.Scala3ProjectDescriptor
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTrait
import org.junit.Assert.*
import org.jetbrains.plugins.scala.project.ScalaLanguageLevel
import org.jetbrains.plugins.scala.project.settings.ScalaCompilerConfiguration

class YamlReferenceResolutionTest extends BasePlatformTestCase:
  override def getProjectDescriptor: LightProjectDescriptor = Scala3ProjectDescriptor

  override def getTestDataPath: String = "src/test/testData"
  
  override def setUp(): Unit =
    super.setUp()
    myFixture.copyDirectoryToProject("scalaCode", "src")
  end setUp

  enum Expected:
    case Package(fqn: String)
    case Type(fqn: String)
    case Yaml(keyText: Option[String])
  import Expected.*

  def testDependentTypeResolution(): Unit =
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |extends: [SbtModule, javalib.PublishModule, mill.scalalib.ScalaModule]
        |
        |object test:
        |  extends: [SbtTests, TestModule.Utest]
        |""".stripMargin
    )

    checkReference("mill.scalalib.ScalaModule", Package("mill"), Package("mill.scalalib"), Type("mill.scalalib.ScalaModule"))
    checkReference("SbtModule", Type("mill.scalalib.SbtModule"))
    checkReference("javalib.PublishModule", Package("mill.javalib"), Type("mill.javalib.PublishModule"))
    checkReference("TestModule.Utest", Type("mill.javalib.TestModule"), Type("mill.javalib.TestModule.Utest"))
    checkReference("SbtTests", Type("mill.scalalib.SbtModule.SbtTests"))
  end testDependentTypeResolution

  def testBuildPrefixResolution(): Unit =
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |extends: [SbtModule]
        |  
        |object test2:
        |  extends: [build.SbtTests]
        |""".stripMargin
    )

    checkReference("build.SbtTests", Yaml(None), Type("mill.scalalib.SbtModule.SbtTests"))
  end testBuildPrefixResolution

  def testNestedResolution(): Unit =
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |object A:
        | extends: [SbtModule]
        | object B:
        |   extends: [SbtTests]
        |""".stripMargin
    )
    
    checkReference("SbtTests", Type("mill.scalalib.SbtModule.SbtTests"))
  end testNestedResolution

  def testExplicitNameResolution(): Unit =
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |object A:
        | extends: [SbtModule]
        |object B:
        | extends: [A.SbtTests]
        |""".stripMargin
    )

    checkReference("A.SbtTests", Yaml(Some("A")), Type("mill.scalalib.SbtModule.SbtTests"))
  end testExplicitNameResolution

  def testExplicitNameResolutionWithBuild(): Unit =
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |object A:
        | extends: [SbtModule]
        |object C:
        | extends: [build.A.SbtTests]
        |""".stripMargin
    )

    checkReference("build.A.SbtTests", Yaml(None), Yaml(Some("A")), Type("mill.scalalib.SbtModule.SbtTests"))
  end testExplicitNameResolutionWithBuild
  
  def testChainedResolution(): Unit =
    myFixture.configureByText(
      "build.mill.yaml",
      """
        |object A:
        | extends: [TheNestiest]
        |object B:
        | extends: [C.TheInner]
        |object C:
        | extends: [A.TheNest]
        |""".stripMargin
    )

    checkReference("A.TheNest", Yaml(Some("A")), Type("mill.scalalib.TheNestiest.TheNest"))
    checkReference("C.TheInner", Yaml(Some("C")), Type("mill.scalalib.TheNestiest.TheNest.TheInner"))
  end testChainedResolution

  //TODO refactor me
  private def checkReference(textToFind: String, expectedSegments: Expected*): Unit =
    val fileText = myFixture.getFile.getText
    val index = fileText.indexOf(textToFind)
    if index == -1 then
      fail(s"Could not find '$textToFind' in file text")
    if index != fileText.lastIndexOf(textToFind) then
      fail(s"Text to find is present in file multiple times")
      
    
    val parts = textToFind.split('.')
    assertEquals(s"Expected same number of segments as parts in '$textToFind'", parts.length, expectedSegments.length)

    var currentOffsetInText = 0
    for (part, expected) <- parts.zip(expectedSegments) do
      val start = currentOffsetInText
      val end = start + part.length
      currentOffsetInText = end + 1 // +1 for the dot

      val targetOffset = index + start
      myFixture.getEditor.getCaretModel.moveToOffset(targetOffset)

      Option(myFixture.getReferenceAtCaretPosition()) match
        case Some(polyRef: PsiPolyVariantReference) =>
          val results = polyRef.multiResolve(false)
          assertEquals(s"Expected exactly 1 resolution result for segment '$part' of '$textToFind' found ${results.map(_.getElement).mkString}", 1, results.length)
          val resolvedElement = results.head.getElement

          expected match
            case Package(fqn) =>
              import com.intellij.psi.PsiPackage
              assertTrue(s"Expected resolved element for '$part' to be PsiPackage, but got ${resolvedElement.getClass.getName}", resolvedElement.isInstanceOf[PsiPackage])
              val psiPackage = resolvedElement.asInstanceOf[PsiPackage]
              assertEquals(fqn, psiPackage.getQualifiedName)

            case Type(fqn) =>
              import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
              assertTrue(s"Expected resolved element for '$part' to be ScTypeDefinition, but got ${resolvedElement.getClass.getName}", resolvedElement.isInstanceOf[ScTypeDefinition])
              val typeDef = resolvedElement.asInstanceOf[ScTypeDefinition]
              assertEquals(fqn, typeDef.qualifiedName)

            case Yaml(keyTextOpt) =>
              import org.jetbrains.yaml.psi.{YAMLMapping, YAMLKeyValue}
              assertTrue(s"Expected resolved element for '$part' to be YAMLMapping, but got ${resolvedElement.getClass.getName}", resolvedElement.isInstanceOf[YAMLMapping])
              val mapping = resolvedElement.asInstanceOf[YAMLMapping]
              keyTextOpt match
                case Some(keyText) =>
                  val parent = mapping.getParent
                  assertTrue(s"Expected parent of YAMLMapping to be YAMLKeyValue, but got ${parent.getClass.getName}", parent.isInstanceOf[YAMLKeyValue])
                  val keyValue = parent.asInstanceOf[YAMLKeyValue]
                  val actualKey = keyValue.getKeyText
                  val cleanedKey = if (actualKey.startsWith("object ")) actualKey.substring(7) else actualKey
                  assertEquals(keyText, cleanedKey)
                case None =>
                  val parent = mapping.getParent
                  assertFalse(s"Expected root YAMLMapping to not be child of YAMLKeyValue", parent.isInstanceOf[YAMLKeyValue])
          end match

        case Some(_) =>
          fail(s"Expected a PsiPolyVariantReference for segment '$part' of '$textToFind' but got a standard one")
        case None =>
          fail(s"No reference found at caret for segment '$part' of '$textToFind'!")
      end match
    end for
  end checkReference
end YamlReferenceResolutionTest
