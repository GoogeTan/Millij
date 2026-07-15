package katze.millij.scalatypes

import katze.millij.data.Smart
import org.jetbrains.plugins.scala.lang.psi.types.api.ParameterizedType
import org.jetbrains.plugins.scala.lang.psi.types.{BaseTypes, ScType, ScTypeExt}

/**
 * Checks if the type is a subtype of scala.collection.immutable.Seq[T] and returns T if so. Otherwise returns None.
 */
def unwrapSeq(tpe: ScType)(using Smart): Option[ScType] =
  val dealiasedType = tpe.removeAliasDefinitions()

  // Если tpe === Seq, то его может не быть в списке предков
  val hierarchyTypes = tpe +: BaseTypes.get(dealiasedType)

  val taskTypeArgOpt = hierarchyTypes.iterator.collectFirst:
    case ParameterizedType(designator, Seq(typeArg))
      if designator.extractClass.exists(_.getQualifiedName == "scala.collection.immutable.Seq") =>
      typeArg

  taskTypeArgOpt
end unwrapSeq
