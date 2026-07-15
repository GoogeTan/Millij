package katze.millij.scalatypes

import katze.millij.data.Smart
import org.jetbrains.plugins.scala.lang.psi.types.api.ParameterizedType
import org.jetbrains.plugins.scala.lang.psi.types.{BaseTypes, ScType, ScTypeExt}

/**
 * Checks if the type is a subtype of mill.api.Task[T] and returns T type if so otherwise returns type unchanged.
 */
def unwrapMillTask(tpe: ScType)(using Smart): ScType =
  val dealiasedType = tpe.removeAliasDefinitions()

  val allTypes = dealiasedType +: BaseTypes.get(dealiasedType)

  val taskTypeArgOpt = allTypes.iterator.collectFirst:
    case ParameterizedType(designator, Seq(typeArg))
      if designator.extractClass.exists(_.getQualifiedName == "mill.api.Task") =>
      typeArg

  taskTypeArgOpt.getOrElse(tpe)
end unwrapMillTask
