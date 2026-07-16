package katze.millij.data

import org.jetbrains.plugins.scala.lang.psi.types.{ScCompoundType, ScType}
import org.jetbrains.plugins.scala.project.ProjectContext

final case class ModuleType[Segment](
  dependencies : List[ResolvedPath[List, Segment, ResolvedSymbol[Segment]]],
  cyclesOfDependnecies : List[DependencyCycle[Segment]]
):
  def ownType(using ProjectContext) : ScCompoundType =
    ScCompoundType(
      components = dependenciesTypes.flatten,
      forceRefinement = true,
      signatureMap = Map(),
      typesMap = Map()
    ).asInstanceOf[ScCompoundType]
  end ownType
  
  def dependenciesTypes(using ProjectContext) : List[Option[ScType]] =
    dependencies.map(
      _.completelyResolvedTarget.flatMap(_.scalaTraitType)
    )
  end dependenciesTypes
end ModuleType
