package katze.millij.util

import katze.millij.path.SegmentedPath
import katze.millij.place.extractObjectName
import katze.millij.psi.PsiChild
import org.jetbrains.yaml.psi.*

import scala.jdk.CollectionConverters.*

def findYamlKeyValue(file: YAMLFile, path: SegmentedPath[List, String]): Option[YAMLKeyValue] =
  val leafName = path.parts.headOption.getOrElse("")

  val res = path.parts.foldRight(Option[YAMLPsiElement](file.getDocuments.getFirst)):
    case (name, Some(currentPlace : YAMLDocument)) =>
      currentPlace.getTopLevelValue match
        case mapping: YAMLMapping =>
          mapping.getKeyValues.asScala.find(kv => extractObjectName(kv.getKeyText).contains(name)).map(_.getValue)
        case kv : YAMLKeyValue if extractObjectName(kv.getKeyText).contains(name) =>
          Some(kv.getValue)
        case _ =>
          None
      end match
    case (name, Some(mapping: YAMLMapping)) =>
      mapping.getKeyValues.asScala.find(kv => extractObjectName(kv.getKeyText).contains(name)).map(_.getValue)
    case (name, _) =>
      None
  end res

  res match
    case Some(PsiChild(_, kv : YAMLKeyValue, _*)) => Some(kv)
    case _ => None
  end match
end findYamlKeyValue