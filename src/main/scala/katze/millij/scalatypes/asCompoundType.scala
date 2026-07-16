package katze.millij.scalatypes

import katze.millij.data.Smart
import org.jetbrains.plugins.scala.lang.psi.types.{ScCompoundType, ScType}
import org.jetbrains.plugins.scala.project.ProjectContext

extension(value : ScType)
  def asCompoundType(using ProjectContext, Smart) : ScCompoundType =
    value match
      case compoundType: ScCompoundType =>
        compoundType
      case _ =>
        ScCompoundType(
          List(value),
          true,
          Map(),
          Map()
        ).asInstanceOf[ScCompoundType]
    end match
  end asCompoundType
end extension
