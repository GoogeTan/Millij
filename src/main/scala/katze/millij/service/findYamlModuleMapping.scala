package katze.millij.service

import com.intellij.psi.search.GlobalSearchScope
import katze.millij.data.Smart
import katze.millij.path.SegmentedPath
import katze.millij.util.findYamlKeyValue
import org.jetbrains.yaml.psi.YAMLMapping

import scala.jdk.CollectionConverters.*

def findYamlModuleMapping(
  scope : GlobalSearchScope,
  path : SegmentedPath[List, String]
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
      findYamlKeyValue(file, inFilePath)
        .map(_.getValue)
        .collect {
          case mapping: YAMLMapping => mapping
        }
  }
end findYamlModuleMapping
