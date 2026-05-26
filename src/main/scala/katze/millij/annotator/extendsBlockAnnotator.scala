package katze.millij.annotator

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.search.GlobalSearchScope
import katze.millij.psi.YAMLKeyValueWithKey
import katze.millij.scalatypes.classTypeSearch
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager
import org.jetbrains.yaml.psi.{YAMLScalar, YAMLSequence, YAMLSequenceItem}

/**
 * Marks unfound classes in extends block with error
 */
def extendsListBlockAnnotator(
  isValidExtendsBlockMember : YAMLScalar => Boolean,
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
    if !isValidExtendsBlockMember(element) then
      annotationHolder
        .newAnnotation(HighlightSeverity.ERROR, s"Couldn't find type ${element.getTextValue}")
        .range(element)
        .create()
end extendsListBlockAnnotator

def isValidExtendsBlockMember(scalar: YAMLScalar) : Boolean =
  val project = scalar.getProject
  classTypeSearch(ScalaPsiManager.instance(project), GlobalSearchScope.allScope(project), scalar.getTextValue).isDefined
end isValidExtendsBlockMember