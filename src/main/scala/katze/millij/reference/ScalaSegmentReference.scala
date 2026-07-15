package katze.millij.reference

import cats.data.NonEmptyList
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import katze.millij.data.*
import katze.millij.data.module.*
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager
import org.jetbrains.yaml.psi.YAMLScalar

import scala.collection.mutable.ArrayBuffer

//TODO document arguments
final class ScalaSegmentReference(
  element: YAMLScalar,
  range: TextRange,
  cumulativePath: SegmentedPath[NonEmptyList, ScalaIdentifier],
  modulePath : NamespacedPath[List, ScalaIdentifier],
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
end ScalaSegmentReference

object ScalaReferenceFactory:
  def makeScalaReferencesFor(psiElement: YAMLScalar, modulePath : NamespacedPath[List, ScalaIdentifier]): Array[PsiReference] =
    val rawText = psiElement.getText
    val textValue = psiElement.getTextValue

    val startOffset = rawText.indexOf(textValue) match
      case -1 => 0
      case n  => n

    val parts = textValue.split('.').map(ScalaIdentifier.fromStringOption).takeWhile(_.isDefined).flatten
    var currentOffset = startOffset
    var cumulativePath: Option[SegmentedPath[NonEmptyList, ScalaIdentifier]] = None
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

      references += new ScalaSegmentReference(psiElement, range, cumulativePath.get, modulePath)
      currentOffset = segmentEnd + 1
    end for

    references.toArray
  end makeScalaReferencesFor
end ScalaReferenceFactory