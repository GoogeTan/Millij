package katze.millij.reference

import cats.data.NonEmptyList
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import katze.millij.data.*
import katze.millij.module.*
import katze.millij.path.*
import katze.millij.service.MillModuleService
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager
import org.jetbrains.yaml.psi.YAMLScalar

import scala.collection.mutable.ArrayBuffer

/**
 * A reference in extends block in mill module in yaml config. Usually 
 * @param element Text element in extends block to search for references
 * @param range A prefix of the element to search for reference in(e.g. word ScalaModule in mill.scalalib.ScalaModule)
 * @param cumulativePath Reference to resolve
 * @param modulePath Enclosing module name. 
 */
final class ExtendsBlockMemberReference(
  element: YAMLScalar,
  range: TextRange,
  cumulativePath: SegmentedPath[NonEmptyList, String],
  modulePath : NamespacedPath[List, String],
) extends PsiPolyVariantReferenceBase[YAMLScalar](element, range, false):
  override def multiResolve(incompleteCode: Boolean): Array[ResolveResult]  =
    val project = getElement.getProject
    Smart(project)[Array[ResolveResult]] {
      project.getService(classOf[MillModuleService]).resolvePath(
        modulePath,
        cumulativePath
      )
        .completelyResolvedTarget
        .map(_.element)
        .map(PsiElementResolveResult(_))
        .toArray
    }.getOrElse(ResolveResult.EMPTY_ARRAY)
  end multiResolve

  override def getVariants: Array[AnyRef] = Array.empty
end ExtendsBlockMemberReference

object ExtendsBlockMemberReference:
  def makeScalaReferencesFor(psiElement: YAMLScalar, modulePath : NamespacedPath[List, String]): Array[PsiReference] =
    val rawText = psiElement.getText
    val textValue = psiElement.getTextValue

    val startOffset = rawText.indexOf(textValue) match
      case -1 => 0
      case n  => n

    val parts = textValue.split('.')
    var currentOffset = startOffset
    var cumulativePath: Option[SegmentedPath[NonEmptyList, String]] = None
    val references = ArrayBuffer[PsiReference]()

    for part <- parts do
      val segmentStart = currentOffset
      val segmentEnd = currentOffset + part.length
      val range = new TextRange(segmentStart, segmentEnd)

      cumulativePath = Some(
        cumulativePath.fold(
          SegmentedPath(NonEmptyList.one(part))
        )(_.addNonEmpty(part))
      )

      references += new ExtendsBlockMemberReference(psiElement, range, cumulativePath.get, modulePath)
      currentOffset = segmentEnd + 1
    end for

    references.toArray
  end makeScalaReferencesFor
end ExtendsBlockMemberReference