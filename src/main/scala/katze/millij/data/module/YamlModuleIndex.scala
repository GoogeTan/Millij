package katze.millij.data.module

import cats.data.NonEmptyList
import com.intellij.lang.{LighterAST, LighterASTNode}
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.tree.{IElementType, IFileElementType}
import com.intellij.util.indexing.*
import com.intellij.util.io.{DataExternalizer, KeyDescriptor}
import katze.millij.data.{ScalaIdentifier, ScalaSegmentedPath, SegmentedPath}
import katze.millij.file.relativePathToContentRoot
import katze.millij.place.{extractObjectName, getExtendsContentsOf}
import org.jetbrains.yaml.{YAMLElementTypes, YAMLTokenTypes}
import org.jetbrains.yaml.psi.*

import java.io.{DataInput, DataOutput}
import java.util
import scala.jdk.CollectionConverters.*


final class YamlModuleIndex extends FileBasedIndexExtension[SegmentedPath[List, ScalaIdentifier], ModuleDeclaration[ScalaIdentifier]]:

  override def getName: ID[SegmentedPath[List, ScalaIdentifier], ModuleDeclaration[ScalaIdentifier]] = YamlModuleIndex.Name

  override def getKeyDescriptor: KeyDescriptor[SegmentedPath[List, ScalaIdentifier]] = SegmentedPathKeyDescriptor

  override def getValueExternalizer: DataExternalizer[ModuleDeclaration[ScalaIdentifier]] = ModuleDeclarationExternalizer()

  override def getVersion: Int = 27

  override def dependsOnFileContent(): Boolean = true

  override def getInputFilter: FileBasedIndex.InputFilter = (file: VirtualFile) =>
    file.getName.endsWith(".mill.yaml")
  end getInputFilter

  override def getIndexer: DataIndexer[SegmentedPath[List, ScalaIdentifier], ModuleDeclaration[ScalaIdentifier], FileContent] =
    (inputData: FileContent) =>
      val map = new util.HashMap[SegmentedPath[List, ScalaIdentifier], ModuleDeclaration[ScalaIdentifier]]()
      val rootPath = rootFileModulePath(inputData).getOrElse(SegmentedPath(Nil))
      val text = inputData.getContentAsText

      inputData match
        case fileContentImpl: com.intellij.util.indexing.FileContentImpl =>
          val tree = fileContentImpl.getLighterAST
          if tree != null then
            collectModulesLighter(
              tree = tree,
              node = tree.getRoot,
              text = text,
              path = NamespacedPath(rootPath, SegmentedPath(Nil)),
              resultConsumer = metadata => map.put(metadata.segmentedPath, metadata)
            )
        case _ =>
          inputData.getPsiFile.getChildren.collect {
            case el: YAMLPsiElement => el
          }.foreach(element =>
            collectModules(
              module = element,
              path = NamespacedPath(rootPath, SegmentedPath(Nil)),
              resultConsumer = metadata => map.put(metadata.segmentedPath, metadata)
            )
          )
      end match

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
  
  def collectModules(
    module : YAMLPsiElement,
    path : NamespacedPath[List, ScalaIdentifier],
    resultConsumer: ModuleDeclaration[ScalaIdentifier] => Unit
  ) : Unit =
    module match
      case document: YAMLDocument =>
        collectModules(document.getTopLevelValue, path, resultConsumer)
      case file: YAMLFile =>
        file.getDocuments.forEach(collectModules(_, path, resultConsumer))
      case value: YAMLKeyValue =>
        extractObjectName(value.getKeyText).foreach(text =>
          collectModules(value.getValue, path.addPathSegment(text), resultConsumer)
        ) 
      case mapping: YAMLMapping =>
        val extendsOf = getExtendsContentsOf(mapping).toList.flatten
                          .flatMap(ScalaSegmentedPath.fromQualifiedNonEmpty)
        resultConsumer(
          ModuleDeclaration(
            path,
            extendsOf
          )
        )
        mapping.getKeyValues.forEach(collectModules(_, path, resultConsumer))
      case _ =>
        ()
    end match
  end collectModules
  
  private def collectModulesLighter(
    tree: LighterAST,
    node: LighterASTNode,
    text: CharSequence,
    path : NamespacedPath[List, ScalaIdentifier],
    resultConsumer: ModuleDeclaration[ScalaIdentifier] => Unit
  ): Unit =
    val tokenType = node.getTokenType
    val children = tree.getChildren(node).asScala.toList

    if tokenType.isInstanceOf[IFileElementType] || tokenType == YAMLElementTypes.DOCUMENT then
      children.foreach(collectModulesLighter(tree, _, text, path, resultConsumer))
    else if tokenType == YAMLElementTypes.KEY_VALUE_PAIR then
      val keyNodeOpt = children.find(c => isScalar(c.getTokenType))
      val valueNodeOpt = children.find(c =>
        val t = c.getTokenType
        t == YAMLElementTypes.MAPPING || t == YAMLElementTypes.SEQUENCE
      )

      val extractedKey = keyNodeOpt.flatMap: k =>
        extractObjectName(text.subSequence(k.getStartOffset, k.getEndOffset).toString)

      extractedKey.zip(valueNodeOpt).foreach(
        (keyText, valNode) =>
          collectModulesLighter(tree, valNode, text, path.addPathSegment(keyText), resultConsumer)
      )

    else if tokenType == YAMLElementTypes.MAPPING then
      val extendsOf = extractExtendsLighter(tree, children, text)

      val currentModule = ModuleDeclaration(
        path,
        extendsOf
      )
      resultConsumer(
        currentModule
      )

      children.foreach(collectModulesLighter(tree, _, text, path, resultConsumer))
  end collectModulesLighter

  private def extractExtendsLighter(
    tree: LighterAST,
    mappingChildren: List[LighterASTNode],
    text: CharSequence
  ): List[SegmentedPath[NonEmptyList, ScalaIdentifier]] =
    val extendsKvOpt = mappingChildren.find: kvNode =>
      if kvNode.getTokenType == YAMLElementTypes.KEY_VALUE_PAIR then
        val kvChildren = tree.getChildren(kvNode).asScala.toList
        val keyNode = kvChildren.find(c => isScalar(c.getTokenType))
        keyNode.exists(k => text.subSequence(k.getStartOffset, k.getEndOffset).toString.trim == "extends")
      else false

    extendsKvOpt match
      case Some(extendsKv) =>
        val kvChildren = tree.getChildren(extendsKv).asScala.toList
        val seqNodeOpt = kvChildren.find(c => c.getTokenType == YAMLElementTypes.SEQUENCE || c.getTokenType == YAMLElementTypes.ARRAY)

        seqNodeOpt match
          case Some(seqNode) =>
            val seqChildren = tree.getChildren(seqNode).asScala.toList
            seqChildren
              .filter(c => c.getTokenType == YAMLElementTypes.SEQUENCE_ITEM)
              .map(item =>
                val raw = text.subSequence(item.getStartOffset, item.getEndOffset).toString.replaceAll("^-", "").trim
                stripQuotes(raw)
              )
              .flatMap(ScalaSegmentedPath.fromQualifiedNonEmpty)
          case None =>
            val keyNodeOpt = kvChildren.find(c => isScalar(c.getTokenType))
            val valNodeOpt = keyNodeOpt.flatMap { keyNode =>
              kvChildren.find(c => isScalar(c.getTokenType) && c != keyNode)
            }
            valNodeOpt match
              case Some(valNode) =>
                val raw = text.subSequence(valNode.getStartOffset, valNode.getEndOffset).toString
                val extendsStr = stripQuotes(raw)
                ScalaSegmentedPath.fromQualifiedNonEmpty(extendsStr).toList
              case None =>
                Nil
      case None =>
        Nil
    end match
  end extractExtendsLighter

  private def stripQuotes(s: String): String =
    val trimmed = s.trim
    if (trimmed.startsWith("\"") && trimmed.endsWith("\"")) || (trimmed.startsWith("'") && trimmed.endsWith("'")) then
      trimmed.substring(1, trimmed.length - 1)
    else
      trimmed
  end stripQuotes

  private def isScalar(tokenType: IElementType): Boolean =
    tokenType == YAMLTokenTypes.SCALAR_KEY ||
      tokenType == YAMLTokenTypes.SCALAR_STRING ||
      tokenType == YAMLTokenTypes.SCALAR_TEXT ||
      tokenType == YAMLTokenTypes.TEXT ||
      tokenType == YAMLElementTypes.SCALAR_PLAIN_VALUE ||
      tokenType == YAMLElementTypes.SCALAR_QUOTED_STRING ||
      tokenType == YAMLElementTypes.SCALAR_TEXT_VALUE ||
      tokenType == YAMLElementTypes.SCALAR_LIST_VALUE
  end isScalar
end YamlModuleIndex

object YamlModuleIndex:
  val Name: ID[SegmentedPath[List, ScalaIdentifier], ModuleDeclaration[ScalaIdentifier]] = ID.create("millij.yaml.modules")
end YamlModuleIndex
