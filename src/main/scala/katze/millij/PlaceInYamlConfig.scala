package katze.millij

import cats.Applicative
import cats.syntax.all.*
import katze.millij.scalatypes.extractTemplateDefinition
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScFunction, ScValue, ScValueOrVariable, ScVariable}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScTypedDefinition
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScClass, ScMember, ScTemplateDefinition}
import org.jetbrains.plugins.scala.lang.psi.types.{BaseTypes, ScType, ScTypeExt}
import org.jetbrains.plugins.scala.lang.psi.types.api.ParameterizedType
import org.jetbrains.yaml.psi.{YAMLDocument, YAMLKeyValue, YAMLMapping, YAMLPsiElement, YAMLScalar, YAMLSequence, YAMLSequenceItem}
import com.intellij.psi.{PsiClass, PsiElement}

import scala.annotation.tailrec
import scala.jdk.CollectionConverters.*

/**
 * Represents a place of file in terms of types. It is either in body of an object or inside a module.
 */
enum PlaceInYamlConfig[Type](val definedMembers : List[String]):
  case Module(
    extendList: List[Type],
    override val definedMembers : List[String]
  ) extends PlaceInYamlConfig[Type](definedMembers)
  
  case Member(
    parentTypes : List[Type], 
    name : String, 
    expectedType: Type, 
    override val definedMembers : List[String]
  ) extends PlaceInYamlConfig[Type](definedMembers)
end PlaceInYamlConfig

/**
 * Attempts to build a place of module defined in element `objectYaml`. If parent place is not a module(
 * which means that user has tried to write a module inside a member) then it returns `ifCalledOnMemberScope`
 */
def nestedModulePlaceFromYamlMapping[F[_] : Applicative, Type](
  parentPlace: PlaceInYamlConfig[Type],
  moduleBody: YAMLMapping,
  classSource : String => F[Option[Type]],
  ifCalledOnMemberScope : PlaceInYamlConfig.Member[Type] => F[PlaceInYamlConfig[Type]]
) : F[PlaceInYamlConfig[Type]] =
  parentPlace match
    case PlaceInYamlConfig.Module(_, _) =>
      modulePlaceOfYamlMapping(moduleBody, classSource)
    case rhs : PlaceInYamlConfig.Member[Type] =>
      ifCalledOnMemberScope(rhs)
end nestedModulePlaceFromYamlMapping

//TODO Rename me
def modulePlaceOfYamlMapping[F[_] : Applicative, Type](
  mapping: YAMLMapping,
  classSource : String => F[Option[Type]]
) : F[PlaceInYamlConfig[Type]] =
  getExtendsContentsOf(mapping).getOrElse(Nil)
    .traverse(classSource)
    .map(_.flatten)
    .map(PlaceInYamlConfig.Module(_, mapping.getKeyValues.asScala.map(_.getKeyText).toList))
end modulePlaceOfYamlMapping

/**
 * Returns all the classes/traits mentioned in extends.
 */
def getExtendsContentsOf(mapping : YAMLMapping) : Option[List[String]] =
  val extendsBlocks = 
    mapping.getKeyValues.asScala.toList
      .map(kv => (kv, kv.getValue))
      .collect:
        case (kv, value : YAMLScalar) if isExtendsBlock(kv.getKeyText) =>
          List(value)
        case (kv, value : YAMLSequence) if isExtendsBlock(kv.getKeyText) =>
          value.getItems.asScala.toList
            .map(_.getValue)
            .collect:
              case element : YAMLScalar => element
  
  if extendsBlocks.length > 1 then
    //TODO log warning down
    ()
  end if
  extendsBlocks.headOption.map(
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
) : F[PlaceInYamlConfig[ScType]] =
  scope match
    case PlaceInYamlConfig.Module(_, _) =>
      onObjectDefinition
    case PlaceInYamlConfig.Member(parentTypes, name, expectedType, _) =>
      unwrapSeq(expectedType)
        .map(PlaceInYamlConfig.Member(parentTypes, name, _, Nil))
        .map(_.pure)
        .getOrElse(onNotSeq)
  end match
end sequenceElementPlace

/**
 * Attempts to build a place of a member. It returns [[None]] only if there is no member with given name.
 */
def memberPlace(scope: PlaceInYamlConfig[ScType], membersName: String): Option[PlaceInYamlConfig[ScType]] =
  def resolveMemberType(baseTypes: List[ScType]): Option[ScType] =
    baseTypes.iterator
      .flatMap(findMemberType(_, membersName))
      .nextOption()
      .map(unwrapMillTask)

  scope match
    case PlaceInYamlConfig.Module(extendList, _) =>
      resolveMemberType(extendList).map(
        PlaceInYamlConfig.Member(extendList, membersName, _, Nil)
      )
    case PlaceInYamlConfig.Member(_, _, expectedType, _) =>
      resolveMemberType(List(expectedType)).map(
        PlaceInYamlConfig.Member(List(expectedType), membersName, _, Nil)
      )
end memberPlace

/**
 * Returns all the members suitable for being defined in a YAML config.
 */
def yamlDefinableMembersOfScope : PlaceInYamlConfig[ScType] => List[ScTypedDefinition] =
  case PlaceInYamlConfig.Module(extendList, _) =>
    extendList
      .flatMap(extractTemplateDefinition)
      .flatMap(getParameterlessMembers)
  case PlaceInYamlConfig.Member(_, _, expectedType, _) =>
    extractTemplateDefinition(expectedType)
      .toList
      .flatMap(getParameterlessMembers)
end yamlDefinableMembersOfScope