package katze.millij.module

import cats.data.NonEmptyList
import katze.millij.path.{NamespacedPath, SegmentedPath}

final case class ModuleDeclaration[Segment](
  path : NamespacedPath[List, Segment],
  superTypes: List[SegmentedPath[NonEmptyList, Segment]]
):
  val segmentedPath: SegmentedPath[List, Segment] = path.fullPath
end ModuleDeclaration
