package katze.millij.path

import cats.*
import cats.data.NonEmptyList
import cats.syntax.all.*

import scala.annotation.targetName

/**
 * A representation of path made of parts e.g. file paths or qualified names. Can both represent
 * non-empty and possibly empty files by substituting corresponding F.
 * @tparam F A collection for parts
 * @tparam Segment A path segment. Might encode correct scala identifier path or correct file path part or might be just a string.
 */
final case class SegmentedPath[F[_], Segment](parts: F[Segment]):
  def length(using UnorderedFoldable[F]) : Long =
    parts.size
  end length

  def add(next: Segment)(using Semigroup[F[Segment]], Applicative[F]): SegmentedPath[F, Segment] =
    SegmentedPath(parts |+| Applicative[F].pure(next))
  end add

  def addNonEmpty(next: Segment)(using Foldable[F]): SegmentedPath[NonEmptyList, Segment] =
    SegmentedPath(NonEmptyList.ofInitLast(parts.toList, next))
  end addNonEmpty

  def +(another: SegmentedPath[F, Segment])(using Semigroup[F[Segment]]): SegmentedPath[F, Segment] =
    SegmentedPath(parts |+| another.parts)
  end +

  def matchLength[G[_] : Foldable](
    another: SegmentedPath[G, Segment]
  )(using Foldable[F], Eq[Segment]): Int =
    parts.toList.iterator.zip(another.parts.toList.iterator).takeWhile(_ === _).length
  end matchLength

  def startsWith[G[_] : Foldable](another: SegmentedPath[G, Segment])(using Foldable[F], Eq[Segment]): Boolean =
    matchLength(another) == another.parts.size.toInt
  end startsWith

  def asQualified(using Foldable[F], Show[Segment]): String =
    parts.mkString_(".")
  end asQualified

  def asFilePath(using Foldable[F], Show[Segment]): String =
    parts.mkString_("/")
  end asFilePath
  
  def mapK[G[_]](f : F ~> G) : SegmentedPath[G, Segment] =
    SegmentedPath(f(parts))
  end mapK
  
  def map[NewSegment](f : Segment => NewSegment)(using Functor[F]) : SegmentedPath[F, NewSegment] =
    SegmentedPath(parts.map(f))
  end map
  
  def traverse[G[_] : Applicative, NewSegment](f : Segment => G[NewSegment])(using Traverse[F]) : G[SegmentedPath[F, NewSegment]] =
    parts.traverse(f).map(SegmentedPath(_))
  end traverse

  def resolve[Target](
    init : Segment => Option[Target],
    rec : (Target, Segment) => Option[Target]
  )(using Foldable[F]) : ResolvedPath[List, Segment, Target] =
    parts.toList match
      case head :: next =>
        next.foldLeft(
          ResolvedPath.makeFirstResolved(head, init(head))
        )((resolvedPart, nextToResolve) =>
          resolvedPart.tryResolveNext(nextToResolve)(rec(_, nextToResolve))
        )
      case Nil =>
        ResolvedPath(Nil, Nil)
  end resolve
  
  def unresolved[Target](using Foldable[F]) : ResolvedPath[List, Segment, Target] =
    ResolvedPath(Nil, parts.toList)
  end unresolved
end SegmentedPath

object SegmentedPath:
  def fromQualified(str: String): SegmentedPath[List, String] =
    val list = if str.isBlank then Nil else str.split('.').toList
    SegmentedPath(list)
  end fromQualified

  def fromQualifiedNonEmpty(str: String): Option[SegmentedPath[NonEmptyList, String]] =
    fromQualified(str).asNonEmpty
  end fromQualifiedNonEmpty

  def fromQualifiedNonEmptyUnsafe(str: String): SegmentedPath[NonEmptyList, String] =
    fromQualifiedNonEmpty(str).get
  end fromQualifiedNonEmptyUnsafe

  def fromPath(str : String) : SegmentedPath[List, String] =
    if str.isBlank then
      SegmentedPath(Nil)
    else
      SegmentedPath(str.split('/').toList)
    end if
  end fromPath

  def fromPathNonEmpty(str : String) : Option[SegmentedPath[NonEmptyList, String]] =
    fromPath(str).asNonEmpty
  end fromPathNonEmpty

  /**
   * Calculates a path and drops the file name(just the last segment)
   * @param str
   * @return
   */
  def folderPath(str: String): SegmentedPath[List, String] =
    val a = fromPath(str)
    if a.parts.nonEmpty then
      SegmentedPath(a.parts.dropRight(1))
    else
      a  
  end folderPath

  extension [F[_] : Foldable, T: Eq](path: SegmentedPath[F, T])
    def commonPartWith[G[_] : Foldable](another: SegmentedPath[G, T]): SegmentedPath[List, T] =
      SegmentedPath(
        path.parts.toList.iterator.zip(another.parts.toList.iterator)
          .takeWhile(_ === _)
          .map(_._1)
          .toList

      )
    end commonPartWith

    def withoutCommonPartWith[G[_] : Foldable](another: SegmentedPath[G, T]): SegmentedPath[List, T] =
      val commonLen = path.matchLength(another)
      SegmentedPath(path.parts.toList.drop(commonLen))
    end withoutCommonPartWith
  end extension

  extension[Segment](path : SegmentedPath[List, Segment])
    def asNonEmpty : Option[SegmentedPath[NonEmptyList, Segment]] =
      path.parts.toNel.map(SegmentedPath(_))
    end asNonEmpty
  end extension

  given eqInstance[F[_], Segment](using Eq[F[Segment]]): Eq[SegmentedPath[F, Segment]] =
    Eq.by(_.parts)
  end eqInstance
end SegmentedPath