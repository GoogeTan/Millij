package katze.millij.reference

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.TextRange
import com.intellij.psi.{JavaPsiFacade, PsiElement, PsiReference, PsiReferenceBase}
import org.jetbrains.plugins.scala.lang.psi.api.expr.ScReferenceExpression
import org.jetbrains.plugins.scala.lang.psi.impl.{ScalaPsiElementFactory, ScalaPsiManager}
import org.jetbrains.yaml.psi.YAMLScalar

import java.util.concurrent.CancellationException
import scala.collection.mutable.ArrayBuffer

final class ScalaSegmentReference(
  element: YAMLScalar,
  range: TextRange,
  cumulativePath: String
) extends PsiReferenceBase[YAMLScalar](element, range, false):

  override def resolve(): PsiElement =
    val project = getElement.getProject
    if DumbService.isDumb(project) then return null

    val scope = getElement.getResolveScope
    val facade = JavaPsiFacade.getInstance(project)

    try
      val pkg = facade.findPackage(cumulativePath)
      if pkg != null then return pkg

      val classes = ScalaPsiManager.instance(project).getCachedClasses(scope, cumulativePath)
      if classes.nonEmpty then return classes.head

      val dummyExpr = ScalaPsiElementFactory.createExpressionFromText(cumulativePath, element)(using project)
      dummyExpr match
        case ref: ScReferenceExpression => ref.resolve()
        case _ => null

    catch
      case e: ProcessCanceledException => throw e
      case e: CancellationException => throw e
      case e: Exception =>
        null

  override def getVariants: Array[AnyRef] = Array.empty

object ScalaReferenceFactory:

  def makeScalaReferencesFor(psiElement: YAMLScalar): Array[PsiReference] =

    val rawText = psiElement.getText
    val textValue = psiElement.getTextValue

    val startOffset = rawText.indexOf(textValue) match
      case -1 => 0
      case n  => n

    val parts = textValue.split('.')
    var currentOffset = startOffset
    var cumulativePath = ""
    val references = ArrayBuffer[PsiReference]()

    for part <- parts do
      val segmentStart = currentOffset
      val segmentEnd = currentOffset + part.length
      val range = new TextRange(segmentStart, segmentEnd)

      cumulativePath = if cumulativePath.isEmpty then part else s"$cumulativePath.$part"

      references += new ScalaSegmentReference(psiElement, range, cumulativePath)
      currentOffset = segmentEnd + 1
    end for

    references.toArray
  end makeScalaReferencesFor
end ScalaReferenceFactory