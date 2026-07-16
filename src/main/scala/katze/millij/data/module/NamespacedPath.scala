package katze.millij.data.module

import cats.data.NonEmptyList
import cats.syntax.all.*
import cats.{Applicative, Foldable, Semigroup, Show, Traverse}
import katze.millij.data.{SegmentedPath, foldableToListK}

final case class NamespacedPath[F[_], Segment](namespace : SegmentedPath[F, Segment], path : SegmentedPath[F, Segment]):
  def fullPath(using Semigroup[F[Segment]]) : SegmentedPath[F, Segment] = 
    namespace + path
  end fullPath

  def addPathSegment(segment: Segment)(using Semigroup[F[Segment]], Applicative[F]) : NamespacedPath[F, Segment] =
    copy(path = path.add(segment))
  end addPathSegment

  def searchScopes(using Traverse[F], Show[Segment], Semigroup[F[Segment]]) : NonEmptyList[SegmentedPath[List, Segment]] =
    val ownFullPath = fullPath
    val listNamespace = namespace.mapK(foldableToListK)
    NonEmptyList.ofInitLast(
      path.parts.mapAccumulate(Nil : List[Segment])((currentPath, part) =>
        (currentPath :+ part, listNamespace + SegmentedPath(currentPath :+ part))
      )._2.toList.filter(_ != ownFullPath),
      SegmentedPath(Nil)
    ).distinctBy(_.asQualified)
  end searchScopes

  def asQualified(using Foldable[F], Show[Segment]): String =
    namespace.asQualified + "::" + path.asQualified
  end asQualified
end NamespacedPath
