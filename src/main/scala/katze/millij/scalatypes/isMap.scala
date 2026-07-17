package katze.millij.scalatypes

import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.plugins.scala.lang.psi.types.{ScType, ScTypeExt}
import org.jetbrains.plugins.scala.lang.psi.types.api.ParameterizedType
import cats.syntax.all.*

def isMap(tie : ScType) : Boolean =
  extractMapTypeArguments(tie).isDefined
end isMap

def extractMapTypeArguments(tpe: ScType): Option[(ScType, ScType)] =
  val dealiasedType = tpe.removeAliasDefinitions()

  dealiasedType match
    case ParameterizedType(designator, Seq(keyType, valueType, _*)) =>
      val isMap = designator.extractClass.exists: psiClass =>
        InheritanceUtil.isInheritor(psiClass, "scala.collection.Map")
          || InheritanceUtil.isInheritor(psiClass, "scala.collection.immutable.Map")

      Option.when(isMap)(keyType -> valueType)
    case _ =>
      None
end extractMapTypeArguments

object ScMapType:
  def unapply(scType: ScType) : Option[(key : ScType, value : ScType)] =
    extractMapTypeArguments(scType)
  end unapply
end ScMapType
