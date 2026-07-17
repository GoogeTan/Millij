package katze.millij.place

import cats.*
import cats.syntax.all.*
import katze.millij.scalatypes.isMvnDependency
import katze.millij.data.module.NamespacedPath
import katze.millij.data.{ScalaIdentifier, SegmentedPath, Smart}
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.project.ProjectContext
import org.jetbrains.yaml.psi.{YAMLMapping, YAMLPsiElement}

import scala.jdk.CollectionConverters.*

final class PlaceConfigResolver[F[_] : Applicative](
  filePath : SegmentedPath[List, ScalaIdentifier],
  resolveParent: (NamespacedPath[List, ScalaIdentifier], String) => F[Option[ScType]],
  fieldNotFound: (String, PlaceInYamlConfig[ScType]) => F[PlaceInYamlConfig[ScType]],
  expectedObjectBody: PlaceInYamlConfig.Member[ScType] => F[PlaceInYamlConfig[ScType]],
  expectedRhs: PlaceInYamlConfig[ScType] => F[PlaceInYamlConfig[ScType]],
  expectedSequence: PlaceInYamlConfig[ScType] => F[PlaceInYamlConfig[ScType]],
  unexpectedPsi: List[YAMLPsiElement] => F[PlaceInYamlConfig[ScType]]
)(using Smart, ProjectContext) extends YAMLConfigResolver[F, PlaceInYamlConfig[ScType]]:

  override def field(scope: PlaceInYamlConfig[ScType], name: String): F[PlaceInYamlConfig[ScType]] =
    scope match
      case PlaceInYamlConfig.Member(_, expectedType, _) if isMvnDependency(expectedType) =>
        scope.pure
      case _ =>  
        memberPlace(scope, name).fold(fieldNotFound(name, scope))(_.pure[F])
  end field

  override def module(parent: PlaceInYamlConfig[ScType], name: ScalaIdentifier, modulePsiElement: YAMLMapping): F[PlaceInYamlConfig[ScType]] =
    nestedModulePlaceFromYamlMapping(
      name = name,
      parentPlace = parent,
      moduleBody = modulePsiElement,
      resolveParent = resolveParent,
      ifCalledOnMemberScope = scope => expectedObjectBody(scope)
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
    val topModulePath = NamespacedPath(filePath, SegmentedPath(Nil))
    modulePlaceOfYamlMapping(topModulePath, mapping, resolveParent(topModulePath, _))
  end topLevelModule

  override def mapping(scope: PlaceInYamlConfig[ScType], mapping: YAMLMapping): F[PlaceInYamlConfig[ScType]] =
    scope match
      case PlaceInYamlConfig.Module(extendList, inFilePath, _) =>
        PlaceInYamlConfig.Module(
          extendList = extendList,
          inFilePath = inFilePath,
          definedMembers = mapping.getKeyValues.asScala.map(_.getKeyText).toList
        ).pure[F]
      case PlaceInYamlConfig.Member(name, expectedType, _) =>
        PlaceInYamlConfig.Member(
          name = name,
          expectedType = expectedType,
          definedMembers = mapping.getKeyValues.asScala.map(_.getKeyText).toList
        ).pure[F]
    end match
  end mapping

  override def onUnexpected(got: List[YAMLPsiElement]): F[PlaceInYamlConfig[ScType]] =
    unexpectedPsi(got)
  end onUnexpected
end PlaceConfigResolver

object PlaceConfigResolver:
  type EitherString[A] = Either[String, A]

  def apply(
    filePath : SegmentedPath[List, ScalaIdentifier],
    search: (NamespacedPath[List, ScalaIdentifier], String) => EitherString[Option[ScType]],
  )(using Smart, ProjectContext): PlaceConfigResolver[EitherString] =
    new PlaceConfigResolver[EitherString](
      filePath = filePath,
      resolveParent = search,
      fieldNotFound = (name, scope) => Left(s"Couldn't find field $name in scope $scope"),
      expectedObjectBody = scope => Left(s"Expected object body but got method right hand side ${scope.name} with type ${scope.expectedType}"),
      expectedRhs = scope => Left(s"Expected RHS but got object $scope"),
      expectedSequence = scope => Left(s"Expected sequence scope but got $scope"),
      unexpectedPsi = got => Left(s"Couldn't parse PSI. Seqence ${got.mkString("->")}"), //TODO add more details
    )
  end apply

  def option(
    filePath : SegmentedPath[List, ScalaIdentifier],
    search: (NamespacedPath[List, ScalaIdentifier], String) => Option[ScType],
  )(using Smart, ProjectContext): PlaceConfigResolver[Option] =
    new PlaceConfigResolver[Option](
      filePath = filePath,
      resolveParent = (m, s) => Some(search(m, s)),
      fieldNotFound = (_, _) => None,
      expectedObjectBody = _ => None,
      expectedRhs = _ => None,
      expectedSequence = _ => None,
      unexpectedPsi = _ => None,
    )
  end option
end PlaceConfigResolver
