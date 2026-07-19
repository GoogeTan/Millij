package katze.millij.index

import cats.syntax.all.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.util.indexing.FileBasedIndex
import katze.millij.Scala3ProjectDescriptor
import katze.millij.data.module.YamlModuleIndex
import katze.millij.data.SegmentedPath
import org.junit.Assert.*

import scala.jdk.CollectionConverters.*

class YamlModuleIndexTest extends BasePlatformTestCase:
  override def getProjectDescriptor = Scala3ProjectDescriptor

  def testSingleModuleIsIndexedCorrectly(): Unit =
    myFixture.addFileToProject(
      "build.mill.yaml",
      """
        |object MyModule:
        |  extends: [mill.scalalib.ScalaModule]
        |""".stripMargin
    )

    val expectedKey = SegmentedPath.fromQualified("MyModule")

    val scope = GlobalSearchScope.allScope(getProject)
    val fileBasedIndex = FileBasedIndex.getInstance()

    val values = fileBasedIndex.getValues(
      YamlModuleIndex.Name,
      expectedKey,
      scope
    ).asScala.toList

    assertEquals("Index should contain exactly one metadata entry for 'MyModule'", 1, values.size)

    val indexedModule = values.head
    assertEquals("The indexed path should match", expectedKey, indexedModule.segmentedPath)
  end testSingleModuleIsIndexedCorrectly

  def testMultipleModulesCanBeRetrievedViaKeys(): Unit =
    myFixture.addFileToProject(
      "core/build.mill.yaml",
      """
        |object A:
        |object B:
        |""".stripMargin
    )

    val fileBasedIndex = FileBasedIndex.getInstance()

    val allKeys = fileBasedIndex.getAllKeys(YamlModuleIndex.Name, getProject).asScala.toSet

    val keyA = SegmentedPath.fromQualified("A")
    val keyB = SegmentedPath.fromQualified("B")

    assertTrue("Index should contain Key A", allKeys.contains(keyA))
    assertTrue("Index should contain Key B", allKeys.contains(keyB))
  end testMultipleModulesCanBeRetrievedViaKeys

  def testIndexGracefullyIgnoresNonTargetFiles(): Unit =
    // This file should NOT be indexed because it doesn't end with .mill.yaml
    myFixture.addFileToProject(
      "random_config.yaml",
      """
        |object ShouldBeIgnored:
        |""".stripMargin
    )

    val fileBasedIndex = FileBasedIndex.getInstance()
    val allKeys = fileBasedIndex.getAllKeys(YamlModuleIndex.Name, getProject).asScala.toSet

    val ignoredKey = SegmentedPath.fromQualified("ShouldBeIgnored")
    assertFalse("File should have been ignored by getInputFilter", allKeys.contains(ignoredKey))
  end testIndexGracefullyIgnoresNonTargetFiles
  
  def testNew() =
    val psiFile = myFixture.configureByText(
      "build.mill.yaml",
      """
        |extends: SbtModule
        |
        |object A:
        | extends: [Sbt, SbtTests]
        |""".stripMargin
    )
    val virtualFile = psiFile.getVirtualFile
    val fileBasedIndex = FileBasedIndex.getInstance()
    val allKeys = fileBasedIndex.getFileData(YamlModuleIndex.Name, virtualFile, getProject).asScala
    
    assertEquals(2, allKeys.size)
    val rootPath = SegmentedPath.fromQualified("")
    val pathA = SegmentedPath.fromQualified("A")

    assertTrue(allKeys.contains(rootPath))
    assertTrue(allKeys.contains(pathA))

    val rootModule = allKeys(rootPath)
    val moduleA = allKeys(pathA)

    assertEquals(List(SegmentedPath.fromQualifiedNonEmptyUnsafe("SbtModule")), rootModule.superTypes)
    assertEquals(List(SegmentedPath.fromQualifiedNonEmptyUnsafe("Sbt"), SegmentedPath.fromQualifiedNonEmptyUnsafe("SbtTests")), moduleA.superTypes)
end YamlModuleIndexTest