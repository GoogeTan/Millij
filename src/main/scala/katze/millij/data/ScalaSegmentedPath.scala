package katze.millij.data

import cats.data.NonEmptyList

type ScalaSegmentedPath[F[_]] = SegmentedPath[F, ScalaIdentifier]

object ScalaSegmentedPath:
  def fromQualifiedUnsafe(string: String) : ScalaSegmentedPath[List] =
    SegmentedPath.fromQualified(string)
      .map(ScalaIdentifier.unsafe)
  end fromQualifiedUnsafe
  
  def fromQualified(string: String) : Option[ScalaSegmentedPath[List]] =
    SegmentedPath.fromQualified(string)
      .traverse(ScalaIdentifier.fromStringOption)
  end fromQualified
  
  def fromQualifiedNonEmpty(string: String) : Option[ScalaSegmentedPath[NonEmptyList]] =
    SegmentedPath.fromQualifiedNonEmpty(string)
      .flatMap(_.traverse(ScalaIdentifier.fromStringOption))
  end fromQualifiedNonEmpty

  def fromPath(string: String) : Option[ScalaSegmentedPath[List]] =
    SegmentedPath.fromPath(string)
      .traverse(ScalaIdentifier.fromStringOption)
  end fromPath
  
  def folderPath(string: String) : Option[ScalaSegmentedPath[List]] =
    SegmentedPath.folderPath(string)
      .traverse(ScalaIdentifier.fromStringOption)
  end folderPath

  def fromPathNonEmpty(string: String) : Option[ScalaSegmentedPath[NonEmptyList]] =
    SegmentedPath.fromPathNonEmpty(string)
      .flatMap(_.traverse(ScalaIdentifier.fromStringOption))
  end fromPathNonEmpty
end ScalaSegmentedPath
