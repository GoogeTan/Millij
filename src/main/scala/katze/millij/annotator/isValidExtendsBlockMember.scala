package katze.millij.annotator

import com.intellij.lang.annotation.HighlightSeverity
import katze.millij.data.*
import katze.millij.data.module.*
import katze.millij.psi.YAMLKeyValueWithKey
import org.jetbrains.yaml.psi.{YAMLScalar, YAMLSequence, YAMLSequenceItem}

def isValidExtendsBlockMember(context: NamespacedPath[List, String], scalar: YAMLScalar)(using Smart) : Boolean =
  val project = scalar.getProject
  val searchService = project.getService(classOf[MillModuleService])
  SegmentedPath.fromQualifiedNonEmpty(scalar.getTextValue)
    .flatMap(searchService.resolvePath(context, _).completelyResolvedTarget)
    .isDefined
end isValidExtendsBlockMember