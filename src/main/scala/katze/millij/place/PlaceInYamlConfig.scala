package katze.millij.place

import cats.Applicative
import cats.syntax.all.*
import katze.millij.data.module.NamespacedPath
import katze.millij.data.{ScalaIdentifier, Smart}
import katze.millij.scalatypes.*
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScTypedDefinition
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTemplateDefinition
import org.jetbrains.plugins.scala.lang.psi.types.{ScCompoundType, ScType, TermSignature}
import org.jetbrains.plugins.scala.project.ProjectContext
import org.jetbrains.yaml.psi.*

import scala.jdk.CollectionConverters.*

/**
 * Represents a place of file in terms of types. It is either in body of an object or inside a module.
 */
enum PlaceInYamlConfig[Type](val definedMembers : List[String]):
  case Module(
    extendList: List[Type],
    inFilePath : NamespacedPath[List, ScalaIdentifier],
    override val definedMembers : List[String],
  ) extends PlaceInYamlConfig[Type](definedMembers)
  
  case Member(
    name : String, 
    expectedType: Type, 
    override val definedMembers : List[String]
  ) extends PlaceInYamlConfig[Type](definedMembers)

  override def toString: String =
    this match
      case PlaceInYamlConfig.Module(extendList, inFilePath, definedMembers) =>
        s"PlaceInYamlConfig.Module(name=${inFilePath.asQualified}, extends=[${extendList.mkString(", ")}], defined members=[${definedMembers.mkString(", ")}])"
      case PlaceInYamlConfig.Member(name, expectedType, definedMembers) =>
        s"PlaceInYamlConfig.Member(name=$name, expectedType=$expectedType, defined members=[${definedMembers.mkString(", ")}])"
    end match
  end toString
end PlaceInYamlConfig

/**
 * Attempts to build a place of module defined in element `objectYaml`. If parent place is not a module(
 * which means that user has tried to write a module inside a member) then it returns `ifCalledOnMemberScope`
 */
def nestedModulePlaceFromYamlMapping[F[_] : Applicative, Type](
  name : ScalaIdentifier,
  parentPlace: PlaceInYamlConfig[Type],
  moduleBody: YAMLMapping,
  resolveParent : (NamespacedPath[List, ScalaIdentifier], String) => F[Option[Type]],
  ifCalledOnMemberScope : PlaceInYamlConfig.Member[Type] => F[PlaceInYamlConfig[Type]]
) : F[PlaceInYamlConfig[Type]] =
  parentPlace match
    case PlaceInYamlConfig.Module(_, inFilePath, _) =>
      modulePlaceOfYamlMapping(inFilePath.addPathSegment(name), moduleBody, resolveParent(inFilePath, _))
    case rhs : PlaceInYamlConfig.Member[Type] =>
      ifCalledOnMemberScope(rhs)
end nestedModulePlaceFromYamlMapping

//TODO Rename me
def modulePlaceOfYamlMapping[F[_] : Applicative, Type](
  path: NamespacedPath[List, ScalaIdentifier],
  mapping: YAMLMapping,
  resolveParent : String => F[Option[Type]]
) : F[PlaceInYamlConfig[Type]] =
  getExtendsContentsOf(mapping).getOrElse(Nil)
    .traverse(resolveParent)
    .map(_.flatten)
    .map(PlaceInYamlConfig.Module(_, path, mapping.getKeyValues.asScala.map(_.getKeyText).toList))
end modulePlaceOfYamlMapping

/**
 * Returns all the classes/traits mentioned in extends.
 */
def getExtendsContentsOf(mapping : YAMLMapping) : Option[List[String]] =
  Option(mapping.getKeyValueByKey("extends"))
      .map(kv => (kv, kv.getValue))
      .collect:
        case (kv, value : YAMLScalar) =>
          List(value)
        case (kv, value : YAMLSequence) =>
          value.getItems.asScala.toList
            .map(_.getValue)
            .collect:
              case element : YAMLScalar => element
      .map(
        _.map(_.getTextValue)
      )
end getExtendsContentsOf

/**
 * Attempts to build a place of a sequence item. It expects parent place to have type Seq[T] and returns `onNotSeq`
 * otherwise. In practice, it means that user has written a sequence where some class was expected. If was called on
 * module place returns `onObjectDefinition`.
 */
def sequenceElementPlace[F[_] : Applicative](
  scope : PlaceInYamlConfig[ScType],
  onObjectDefinition : => F[PlaceInYamlConfig[ScType]],
  onNotSeq : => F[PlaceInYamlConfig[ScType]]
)(using Smart) : F[PlaceInYamlConfig[ScType]] =
  scope match
    case PlaceInYamlConfig.Module(_, _, _) =>
      onObjectDefinition
    case PlaceInYamlConfig.Member(name, expectedType, _) =>
      unwrapSeq(expectedType)
        .map(PlaceInYamlConfig.Member(name, _, Nil))
        .map(_.pure)
        .getOrElse(onNotSeq)
  end match
end sequenceElementPlace

/**
 * Attempts to build a place of a member. It returns [[None]] only if there is no member with given name.
 */
def memberPlace(scope: PlaceInYamlConfig[ScType], membersName: String)(using Smart, ProjectContext): Option[PlaceInYamlConfig[ScType]] =
  def resolveMemberType(baseTypes: List[ScType]): Option[ScType] =
    findMemberType(
      ScCompoundType(
        baseTypes,
        true,
        Map(),
        Map()
      ),
      membersName
    ).map(unwrapMillTask)

  scope match
    case PlaceInYamlConfig.Module(extendList, _, _) =>
      resolveMemberType(extendList).map(
        PlaceInYamlConfig.Member(membersName, _, Nil)
      )
    case PlaceInYamlConfig.Member(_, expectedType, _) =>
      resolveMemberType(List(expectedType)).map(
        PlaceInYamlConfig.Member(membersName, _, Nil)
      )
end memberPlace

/**
 * Returns all the members suitable for being defined in a YAML config.
 */
def yamlDefinableMembersOfScope(using Smart, ProjectContext) : PlaceInYamlConfig[ScType] => Seq[TermSignature] =
  case PlaceInYamlConfig.Module(extendList, _, _) =>
    extendList
      .flatMap(yamlDefinableMembersOf)
  case PlaceInYamlConfig.Member(_, expectedType, _) =>
    yamlDefinableMembersOf(expectedType)
end yamlDefinableMembersOfScope