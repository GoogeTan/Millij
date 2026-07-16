package katze.millij.data.module

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.util.indexing.*
import com.intellij.util.io.{DataExternalizer, KeyDescriptor}
import katze.millij.data.{ScalaIdentifier, SegmentedPath}
import katze.millij.file.relativePathToContentRoot
import katze.millij.place.{extractObjectName, getExtendsContentsOf}
import org.jetbrains.yaml.psi.*

import java.io.{DataInput, DataOutput}
import scala.jdk.CollectionConverters.*


final class YamlModuleIndex extends FileBasedIndexExtension[SegmentedPath[List, ScalaIdentifier], ModuleDeclaration[ScalaIdentifier]]:

  override def getName: ID[SegmentedPath[List, ScalaIdentifier], ModuleDeclaration[ScalaIdentifier]] = YamlModuleIndex.Name

  override def getKeyDescriptor: KeyDescriptor[SegmentedPath[List, ScalaIdentifier]] =
    new KeyDescriptor[SegmentedPath[List, ScalaIdentifier]]:
      override def getHashCode(t: SegmentedPath[List, ScalaIdentifier]): Int =
        t.hashCode()
      end getHashCode
      
      override def save(dataOutput: DataOutput, t: SegmentedPath[List, ScalaIdentifier]): Unit =
        dataOutput.writeUTF(t.asQualified)
      end save

      override def read(dataInput: DataInput): SegmentedPath[List, ScalaIdentifier] =
        SegmentedPath.fromQualified(dataInput.readUTF()).traverse(ScalaIdentifier.fromStringOption).get//TODO
      end read

      override def isEqual(t: SegmentedPath[List, ScalaIdentifier], t1: SegmentedPath[List, ScalaIdentifier]): Boolean =
        t == t1
      end isEqual
    end new
  end getKeyDescriptor

  override def getValueExternalizer: DataExternalizer[ModuleDeclaration[ScalaIdentifier]] = ModuleDeclarationExternalizer()

  override def getVersion: Int = 25

  override def dependsOnFileContent(): Boolean = true

  override def getInputFilter: FileBasedIndex.InputFilter = (file: VirtualFile) =>
    file.getName.endsWith(".mill.yaml")
  end getInputFilter

  override def getIndexer: DataIndexer[SegmentedPath[List, ScalaIdentifier], ModuleDeclaration[ScalaIdentifier], FileContent] =
    (inputData: FileContent) =>
      val map = new java.util.HashMap[SegmentedPath[List, ScalaIdentifier], ModuleDeclaration[ScalaIdentifier]]()
      val rootPath = rootFileModulePath(inputData).get//TODO
      val psiFile = inputData.getPsiFile
      psiFile.getChildren.collect {
        case el : YAMLPsiElement => el
      }.foreach(element =>
        collectModules(element, rootPath, SegmentedPath(Nil)).foreach(metadata =>
          map.put(metadata.segmentedPath, metadata)
        )
      )
      
      map
  end getIndexer

  def rootFileModulePath(inputData : FileContent) : Option[SegmentedPath[List, ScalaIdentifier]] =
    inputData.relativePathToContentRoot.flatMap(relativePath =>
      if relativePath.contains('/') then
        val parentPath = relativePath.substring(0, relativePath.lastIndexOf('/'))
        SegmentedPath.fromQualified(parentPath.replace('/', '.')).traverse(ScalaIdentifier.fromStringOption)
      else
        Some(SegmentedPath(Nil))
      end if
    )
  end rootFileModulePath

  def collectModules(module : YAMLPsiElement, filePath : SegmentedPath[List, ScalaIdentifier], inFilePath : SegmentedPath[List, ScalaIdentifier]) : List[ModuleDeclaration[ScalaIdentifier]] =
    module match
      case document: YAMLDocument =>
        collectModules(document.getTopLevelValue, filePath, inFilePath)
      case file: YAMLFile =>
        file.getDocuments.asScala.toList.flatMap(collectModules(_, filePath, inFilePath))
      case value: YAMLKeyValue =>
        extractObjectName(value.getKeyText) match
          case Some(text) =>
            collectModules(value.getValue, filePath, inFilePath.add(text))
          case None =>
            Nil
      case mapping: YAMLMapping =>
        val extendsOf = getExtendsContentsOf(mapping).toList.flatten
                          .flatMap(SegmentedPath.fromQualifiedNonEmpty)
                          .flatMap(_.traverse(ScalaIdentifier.fromStringOption))
        ModuleDeclaration(
          NamespacedPath(
            filePath,
            inFilePath,
          ),
          extendsOf
        ) :: mapping.getKeyValues.asScala.toList.flatMap(collectModules(_, filePath, inFilePath))
      case _ =>
        Nil
    end match
  end collectModules
end YamlModuleIndex

object YamlModuleIndex:
  val Name: ID[SegmentedPath[List, ScalaIdentifier], ModuleDeclaration[ScalaIdentifier]] = ID.create("millij.yaml.modules")
end YamlModuleIndex
