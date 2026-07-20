package katze.millij.annotator.annotators

import com.intellij.lang.annotation.HighlightSeverity
import katze.millij.data.*
import katze.millij.module.*
import katze.millij.path.*
import katze.millij.psi.YAMLKeyValueWithKey
import katze.millij.service.MillModuleService
import org.jetbrains.yaml.psi.{YAMLScalar, YAMLSequence, YAMLSequenceItem}

def isValidExtendsBlockMember(context: NamespacedPath[List, String], scalar: YAMLScalar)(using Smart) : Boolean =
  val project = scalar.getProject
  val searchService = project.getService(classOf[MillModuleService])
  SegmentedPath.fromQualifiedNonEmpty(scalar.getTextValue)
    .flatMap(searchService.resolvePath(context, _).completelyResolvedTarget)
    .isDefined
end isValidExtendsBlockMember