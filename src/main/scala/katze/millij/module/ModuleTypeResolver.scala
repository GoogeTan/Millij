package katze.millij.module

import cats.data.NonEmptyList
import katze.millij.data.{ResolvedSymbol, Smart}
import katze.millij.path.*

trait ModuleTypeResolver[Segment]:
  def typeModule(module: ModuleDeclaration[Segment])(using Smart) : ModuleType[Segment]

  /**
   * Resolves qualified identifier in context of a module. It resolves both scala
   * and yaml identifiers.
   *
   * The method first resolved the first part of the path. Then it resolves other
   * parts as members of the first found part.
   *
   * The search for first part goes in given order:
   * - if the module is nested in its file then all the surrounding modules will be checked to have
   *   required first element of the path.
   * - Then root `build` module will be checked
   * - Then the path will be interpreted as scala identifier and searched accordingly
   *
   * @param module In which module the identifier was written.
   * @param path A path to resolve.
   * @return
   */
  def resolvePath(
    module: NamespacedPath[List, Segment],
    path: SegmentedPath[NonEmptyList, Segment]
  )(using Smart): ResolvedPath[List, Segment, ResolvedSymbol[Segment]]
end ModuleTypeResolver
