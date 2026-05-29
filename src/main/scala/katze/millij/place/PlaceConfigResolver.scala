package katze.millij.place

import cats.*
import cats.syntax.all.*
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.yaml.psi.{YAMLMapping, YAMLPsiElement}

import scala.jdk.CollectionConverters.*

final class PlaceConfigResolver[F[_] : Applicative](
  search: String => F[Option[ScType]],
  fieldNotFound: (String, PlaceInYamlConfig[ScType]) => F[PlaceInYamlConfig[ScType]],
  expectedObjectBody: PlaceInYamlConfig.Member[ScType] => F[PlaceInYamlConfig[ScType]],
  expectedRhs: PlaceInYamlConfig[ScType] => F[PlaceInYamlConfig[ScType]],
  expectedSequence: PlaceInYamlConfig[ScType] => F[PlaceInYamlConfig[ScType]],
  unexpectedPsi: YAMLPsiElement => F[PlaceInYamlConfig[ScType]]
) extends YAMLConfigResolver[F, PlaceInYamlConfig[ScType]]:

  override def field(scope: PlaceInYamlConfig[ScType], name: String): F[PlaceInYamlConfig[ScType]] =
    memberPlace(scope, name).fold(fieldNotFound(name, scope))(_.pure[F])
  end field

  override def module(parent: PlaceInYamlConfig[ScType], name: String, modulePsiElement: YAMLMapping): F[PlaceInYamlConfig[ScType]] =
    nestedModulePlaceFromYamlMapping(
      parent,
      modulePsiElement,
      search,
      scope => expectedObjectBody(scope)
    )
  end module

  override def sequenceItem(scope: PlaceInYamlConfig[ScType]): F[PlaceInYamlConfig[ScType]] =
    sequenceElementPlace[F](
      scope = scope,
      onObjectDefinition = expectedRhs(scope),
      onNotSeq = expectedSequence(scope)
    )
  end sequenceItem

  override def topLevelModule(mapping: YAMLMapping): F[PlaceInYamlConfig[ScType]] =
    modulePlaceOfYamlMapping(mapping, search)
  end topLevelModule

  override def mapping(scope: PlaceInYamlConfig[ScType], mapping: YAMLMapping): F[PlaceInYamlConfig[ScType]] =
    scope match
      case PlaceInYamlConfig.Module(extendList, _) =>
        PlaceInYamlConfig.Module(
          extendList = extendList,
          definedMembers = mapping.getKeyValues.asScala.map(_.getKeyText).toList
        ).pure[F]
      case PlaceInYamlConfig.Member(parentTypes, name, expectedType, _) =>
        PlaceInYamlConfig.Member(
          parentTypes = parentTypes,
          name = name,
          expectedType = expectedType,
          definedMembers = mapping.getKeyValues.asScala.map(_.getKeyText).toList
        ).pure[F]
    end match
  end mapping

  override def onUnexpected(got: YAMLPsiElement): F[PlaceInYamlConfig[ScType]] =
    unexpectedPsi(got)
  end onUnexpected
end PlaceConfigResolver

object PlaceConfigResolver:
  type EitherString[A] = Either[String, A]

  def apply(
    search: String => Either[String, Option[ScType]]
  ): PlaceConfigResolver[EitherString] =
    new PlaceConfigResolver[EitherString](
      search = search,
      fieldNotFound = (name, scope) => Left(s"Couldn't find field $name in scope $scope"),
      expectedObjectBody = scope => Left(s"Expected object body but got method right hand side ${scope.name} with type ${scope.expectedType}"),
      expectedRhs = scope => Left(s"Expected RHS but got object $scope"),
      expectedSequence = scope => Left(s"Expected sequence scope but got $scope"),
      unexpectedPsi = got => Left(s"Couldn't parse PSI. Got $got of class ${got.getClass} with parent ${got.getParent}")
    )
  end apply

  def option(
    search: String => Option[ScType]
  ): PlaceConfigResolver[Option] =
    new PlaceConfigResolver[Option](
      search = s => Some(search(s)),
      fieldNotFound = (_, _) => None,
      expectedObjectBody = _ => None,
      expectedRhs = _ => None,
      expectedSequence = _ => None,
      unexpectedPsi = _ => None
    )
  end option
end PlaceConfigResolver
