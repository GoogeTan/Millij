package katze.millij.path

import cats.data.NonEmptyList
import cats.syntax.all.*
import cats.{Applicative, Eq, Foldable, SemigroupK, ~>}
import katze.millij.data.ResolvedSymbol

/**
 * Is used to represent an identifier whose parts are resolved.
 *
 * @example In `cats.Monad` resolved would be `NonEmptyList((cats, psiElementOfCatsPackage), (Monad, psiElementOfMonadClass))`
 * @param resolved resolved part of the path
 * @param unresolved unresolved part. (e.g. `List("nonsense")` for `cats.nonsense`
 * @tparam F A type of list for resolved paths. Sometimes ResolvedPath with at least some resolved part may be needed.
 * @tparam PathMember A path member. It is usually a [[String]] or [[ScalaIdentifier]]
 * @tparam Target Something that represents resolved identifier. It may be [[com.intellij.psi.PsiElement]] or [[ResolvedSymbol]]
 */
final case class ResolvedPath[F[_], PathMember, Target](
  resolved: F[(name: PathMember, resolutionTarget: Target)],
  unresolved: List[PathMember]
):
  def atPath(path : SegmentedPath[NonEmptyList, PathMember])(using Eq[PathMember], Foldable[F], Applicative[F], SemigroupK[F]) : Option[Target] =
    if segmentedPath.startsWith(path) then 
      resolved.get(path.length - 1).map(_.resolutionTarget)
    else
      None
    end if
  end atPath
  
  def hasResolvedSomething(using Foldable[F]): Boolean =
    resolved.nonEmpty
  end hasResolvedSomething

  def isCompletelyResolved: Boolean =
    unresolved.isEmpty
  end isCompletelyResolved

  def countResolved(using Foldable[F]): Int =
    resolved.size.toInt
  end countResolved

  def completelyResolvedTarget(using Foldable[F]): Option[Target] =
    if isCompletelyResolved then
      resolved.toList.lastOption.map(_.resolutionTarget)
    else
      None
    end if
  end completelyResolvedTarget

  def segmentedPath(using A : Applicative[F], SK : SemigroupK[F]): SegmentedPath[F, PathMember] =
    NonEmptyList.fromList(unresolved).fold(
      SegmentedPath(resolved.map(_.name))
    )(nonEmptyListOfUnresolvedNames =>
      SegmentedPath(
        SK.combineK(
          resolved.map(_.name),
          nonEmptyListOfUnresolvedNames.map(A.pure).reduceK
        )
      )
    )
  end segmentedPath

  def tryResolveNext(
    name: PathMember
  )(
    resolution: Target => Option[Target]
  )(using
    Foldable[F],
    SemigroupK[F],
    Applicative[F]
  ): ResolvedPath[F, PathMember, Target] =
    completelyResolvedTarget.fold(
      ResolvedPath(resolved, unresolved :+ name)
    )(theLastResolved =>
      val newOne = resolution(theLastResolved)
      newOne.fold(
        ResolvedPath(resolved, List(name))
      )(newResolved =>
        ResolvedPath(resolved <+> (name, newResolved).pure, Nil)
      )
    )
  end tryResolveNext

  def mapK[G[_]](f : F ~> G) : ResolvedPath[G, PathMember, Target] =
    ResolvedPath(f(resolved), unresolved)
  end mapK
end ResolvedPath

object ResolvedPath:
  def makeFirstResolved[PathMember, T](name: PathMember, element: Option[T]): ResolvedPath[List, PathMember, T] =
    element.fold(
      ResolvedPath(Nil, List(name))
    )(foundElement =>
      ResolvedPath(List((name, foundElement)), Nil)
    )
  end makeFirstResolved
end ResolvedPath