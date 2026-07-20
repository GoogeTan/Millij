package katze.millij.module

import cats.*
import cats.data.NonEmptyList
import cats.syntax.all.*
import katze.millij.path.SegmentedPath

final case class DependencyCycle[Segment](cycle : NonEmptyList[SegmentedPath[List, Segment]]):
  def isOnCycle(module : SegmentedPath[List, Segment])(using Eq[Segment]) : Boolean =
    cycle.exists(_ === module)
  end isOnCycle

  def whatModuleDependsOn(module : SegmentedPath[List, Segment])(using Eq[Segment]) : Option[SegmentedPath[List, Segment]] =
    if module === cycle.head then
      Some(cycle.last)
    else
      cycle.iterator.sliding(2).collectFirst:
        case Seq(dependency, current) if current === module => dependency
  end whatModuleDependsOn
end DependencyCycle
