package katze.millij.place

import cats.data.Chain
import cats.syntax.all.*
import katze.millij.data.ScalaIdentifier
import katze.millij.place.{TestPlace, YAMLConfigResolver}
import org.jetbrains.yaml.psi.{YAMLMapping, YAMLPsiElement}

object TestPlaceYAMLConfigResolver extends YAMLConfigResolver[[X] =>> Either[Chain[String], X], TestPlace]:
  type F[T] = Either[Chain[String], T]
  
  override def field(parent: TestPlace, name: String): F[TestPlace] = Right(TestPlace.MemberOf(parent, name))

  override def module(parent: TestPlace, name: ScalaIdentifier, mapping: YAMLMapping): F[TestPlace] =
    extendsOf(mapping)
      .leftMap(_.map(_.toString))
      .map(opt => TestPlace.ClassFromName(opt.getOrElse(List.empty)))

  override def sequenceItem(parent: TestPlace): F[TestPlace] = Right(TestPlace.UnSeqOf(parent))

  override def topLevelModule(mapping: YAMLMapping): F[TestPlace] =
    extendsOf(mapping)
      .leftMap(_.map(_.toString))
      .map(opt => TestPlace.ClassFromName(opt.getOrElse(List.empty)))

  override def mapping(parent: TestPlace, mapping: YAMLMapping): F[TestPlace] = Right(parent)

  override def onUnexpected(element: List[YAMLPsiElement]): F[TestPlace] = Left(Chain(s"Got unexpected ${element.mkString("->")}"))
end TestPlaceYAMLConfigResolver
