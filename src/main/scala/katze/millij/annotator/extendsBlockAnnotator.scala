package katze.millij.annotator

import com.intellij.lang.annotation.HighlightSeverity
import katze.millij.data.*
import katze.millij.data.module.*
import katze.millij.psi.YAMLKeyValueWithKey
import org.jetbrains.yaml.psi.{YAMLScalar, YAMLSequence, YAMLSequenceItem}

/**
 * Marks unfound classes in extends block with error
 */
def extendsListBlockAnnotator(
  extendsBlockError : YAMLScalar => Option[String],
): CoolAnnotator[
  (
    YAMLScalar,
    Either[
      (YAMLSequenceItem, YAMLSequence, YAMLKeyValueWithKey["extends"]),
      YAMLKeyValueWithKey["extends"]
    ]
  )
] =
  case ((element, _), annotationHolder) =>
    extendsBlockError(element).foreach(error =>
      annotationHolder
        .newAnnotation(HighlightSeverity.ERROR, error)
        .range(element)
        .create()
    )
end extendsListBlockAnnotator

def isValidExtendsBlockMember(context: NamespacedPath[List, ScalaIdentifier], scalar: YAMLScalar)(using Smart) : Boolean =
  val project = scalar.getProject
  val searchService = project.getService(classOf[MillModuleService])
  SegmentedPath
    .fromQualifiedNonEmpty(scalar.getTextValue)
    .flatMap(_.traverse(ScalaIdentifier.fromStringOption))
    .flatMap(searchService.resolvePath(context, _).completelyResolvedTarget)
    .isDefined
end isValidExtendsBlockMember