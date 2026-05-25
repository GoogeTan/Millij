package katze.millij.scalatypes

import org.jetbrains.plugins.scala.lang.psi.types.{BaseTypes, ScType}
import org.jetbrains.plugins.scala.lang.psi.types.api.ParameterizedType
import org.jetbrains.plugins.scala.lang.psi.types.ScTypeExt

/**
 * Checks if the type is a subtype of mill.api.Task[T] and returns T type if so otherwise returns type unchanged.
 */
def unwrapMillTask(tpe: ScType): ScType =
  val dealiasedType = tpe.removeAliasDefinitions()

  val hierarchyTypes = BaseTypes.get(dealiasedType)

  val taskTypeArgOpt = hierarchyTypes.iterator.collectFirst:
    case ParameterizedType(designator, Seq(typeArg))
      if designator.extractClass.exists(_.getQualifiedName == "mill.api.Task") =>
      typeArg

  taskTypeArgOpt.getOrElse(tpe)
end unwrapMillTask
