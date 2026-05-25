package katze.millij.place

import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.yaml.psi.{YAMLMapping, YAMLPsiElement}
import scala.jdk.CollectionConverters.*

class PlaceConfigResolver(
  search : String => Either[String, Option[ScType]]
) extends YAMLConfigResolver[[X] =>> Either[String, X], PlaceInYamlConfig[ScType]]:
  override def field(scope: PlaceInYamlConfig[ScType], name: String): Either[String, PlaceInYamlConfig[ScType]] =
    memberPlace(scope, name).toRight(s"Couldn't find field $name in scope $scope")
  end field

  override def module(parent: PlaceInYamlConfig[ScType], name: String, modulePsiElement: YAMLMapping) : Either[String, PlaceInYamlConfig[ScType]] =
    nestedModulePlaceFromYamlMapping(
      parent,
      modulePsiElement,
      search,
      scope => Left(s"Expected object body but got method right hand side ${scope.name} with type ${scope.expectedType}")
    )
  end module

  override def sequenceItem(scope: PlaceInYamlConfig[ScType]): Either[String, PlaceInYamlConfig[ScType]] =
    sequenceElementPlace[[T] =>> Either[String, T]](
      scope = scope,
      onObjectDefinition = Left(s"Expected RHS but got object $scope"),
      onNotSeq = Left(s"Expected sequence scope but got $scope"),
    )
  end sequenceItem

  override def topLevelModule(mapping: YAMLMapping): Either[String, PlaceInYamlConfig[ScType]] =
    modulePlaceOfYamlMapping(mapping, search)
  end topLevelModule

  override def mapping(scope: PlaceInYamlConfig[ScType], mapping: YAMLMapping): Either[String, PlaceInYamlConfig[ScType]] =
    scope match
      case PlaceInYamlConfig.Module(extendList, definedMembers) =>
        Right(
          PlaceInYamlConfig.Module(
            extendList = extendList,
            definedMembers = mapping.getKeyValues.asScala.map(_.getKeyText).toList
          )
        )
      case PlaceInYamlConfig.Member(parentTypes, name, expectedType, definedMembers) =>
        Right(
          PlaceInYamlConfig.Member(
            parentTypes = parentTypes,
            name = name,
            expectedType = expectedType,
            definedMembers = mapping.getKeyValues.asScala.map(_.getKeyText).toList
          )
        )
    end match
  end mapping

  override def onUnexpected(got: YAMLPsiElement): Either[String, PlaceInYamlConfig[ScType]] =
    Left(s"Couldn't parse PSI. Got $got of class ${got.getClass} with parent ${got.getParent}")
  end onUnexpected
end PlaceConfigResolver