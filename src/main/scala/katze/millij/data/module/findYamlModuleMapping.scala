package katze.millij.data.module

import com.intellij.psi.search.GlobalSearchScope
import katze.millij.data.{ScalaIdentifier, SegmentedPath, Smart}
import katze.millij.util.findYamlKeyValue
import org.jetbrains.yaml.psi.YAMLMapping

import scala.jdk.CollectionConverters.*

def findYamlModuleMapping(
  scope : GlobalSearchScope,
  path : SegmentedPath[List, ScalaIdentifier]
)(
  using Smart
) : Option[YAMLMapping] =
  fileByModuleName(scope, path).headOption.flatMap { (metadata, file) =>
    val inFilePath = metadata.path.path
    if inFilePath.parts.isEmpty then
      file.getDocuments.asScala.headOption.map(_.getTopLevelValue).collect {
        case mapping: YAMLMapping => mapping
      }
    else
      findYamlKeyValue(file, inFilePath.map(_.asString))
        .map(_.getValue)
        .collect {
          case mapping: YAMLMapping => mapping
        }
  }
end findYamlModuleMapping
