package katze.millij

import cats.Monad
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

enum CurrentScope[Type](val definedMembers : List[String]):
  case ObjectDefinition(
    extendList: List[Type],
    override val definedMembers : List[String]
  ) extends CurrentScope[Type](definedMembers)
  
  case OverrideRightHandSide(
    parentTypes : List[Type], 
    name : String, 
    expectedType: Type, 
    override val definedMembers : List[String]
  ) extends CurrentScope[Type](definedMembers)
end CurrentScope

def objectScope[F[_] : Monad, Type](
  scope: CurrentScope[Type],
  objectName: String,
  objectYaml: YAMLMapping,
  classSource : String => F[Option[Type]],
  onRightHandSide : CurrentScope.OverrideRightHandSide[Type] => F[CurrentScope[Type]]
) : F[CurrentScope[Type]] =
  scope match
    case CurrentScope.ObjectDefinition(extendList, _) =>
      extendsOf(objectYaml, classSource)
    case rhs : CurrentScope.OverrideRightHandSide[ScType] =>
      onRightHandSide(rhs)
end objectScope

//TODO Rename me
def extendsOf[F[_] : Monad, Type](
  mapping: YAMLMapping,
  classSource : String => F[Option[Type]]
) : F[CurrentScope[Type]] =
  mapping.getKeyValues.asScala.toList.collect {
    case kv : YAMLKeyValue if isExtendsBlock(kv.getKeyText) && kv.getValue.isInstanceOf[YAMLScalar] =>
      List(kv.getValue.asInstanceOf[YAMLScalar])
    case kv : YAMLKeyValue if isExtendsBlock(kv.getKeyText) && kv.getValue.isInstanceOf[YAMLSequence] =>
      kv.getValue.asInstanceOf[YAMLSequence].getItems.asScala.flatMap {
        case element : YAMLSequenceItem if element.getValue.isInstanceOf[YAMLScalar] =>
          List(element.getValue.asInstanceOf[YAMLScalar])
        case _ =>
          Nil
      }
  }
    .flatten
    .map(_.getTextValue)
    .traverse(classSource)
    .map(_.flatten)
    .map(CurrentScope.ObjectDefinition(_, mapping.getKeyValues.asScala.map(_.getKeyText).toList))
end extendsOf

def sequenceScope[F[_] : Monad](
  scope : CurrentScope[ScType],
  onObjectDefinition : F[CurrentScope[ScType]],
  onNotSeq : F[CurrentScope[ScType]]
) : F[CurrentScope[ScType]] =
  scope match
    case CurrentScope.ObjectDefinition(extendList, _) =>
      onObjectDefinition
    case CurrentScope.OverrideRightHandSide(parentTypes, name, expectedType, _) =>
      unwrapSeq(expectedType)
        .map(CurrentScope.OverrideRightHandSide(parentTypes, name, _, Nil))
        .map(_.pure)
        .getOrElse(onNotSeq)
  end match
end sequenceScope

def fieldScope(scope: CurrentScope[ScType], field: String): Option[CurrentScope[ScType]] =
  def resolveType(baseTypes: List[ScType]): Option[ScType] =
    baseTypes.iterator
      .flatMap(findMemberType(_, field))
      .nextOption()
      .map(unwrapMillTask)

  scope match
    case CurrentScope.ObjectDefinition(extendList, _) =>
      resolveType(extendList).map(
        CurrentScope.OverrideRightHandSide(extendList, field, _, Nil)
      )
    case CurrentScope.OverrideRightHandSide(_, _, expectedType, _) =>
      resolveType(List(expectedType)).map(
        CurrentScope.OverrideRightHandSide(List(expectedType), field, _, Nil)
      )
end fieldScope

/**
 * Searches the base ScType for a method with 0 arguments, a field matching the name,
 * or a primary constructor parameter.
 */
def findMemberType(baseType: ScType, fieldName: String): Option[ScType] =
  val cleanFieldName = fieldName.trim.stripSuffix(":")//TODO Удалить эту строку, так как думать об этом явно не задача местного кода

  extractTemplateDefinition(baseType).flatMap { template =>
    val directMatch = template.members.iterator.collectFirst {
      case fn: ScFunction if fn.parameters.isEmpty && fn.name == cleanFieldName =>
        fn.returnType.toOption

      case v: ScValue if v.declaredElements.exists(_.name == cleanFieldName) =>
        v.declaredElements.find(_.name == cleanFieldName).flatMap(_.`type`().toOption)

      case v: ScVariable if v.declaredElements.exists(_.name == cleanFieldName) =>
        v.declaredElements.find(_.name == cleanFieldName).flatMap(_.`type`().toOption)
    }.flatten

    val constructorMatch = template match
      case clazz: ScClass =>
        clazz.parameters.find(_.name == cleanFieldName).flatMap(_.`type`().toOption)
      case _ =>
        None

    lazy val inheritedMatch = template.allMethods.iterator.map(_.method).collectFirst {
      case fn: ScFunction if fn.parameters.isEmpty && fn.name == cleanFieldName =>
        fn.returnType.toOption
    }.flatten

    directMatch.orElse(constructorMatch).orElse(inheritedMatch)
  }
end findMemberType

/**
 * Checks if the type is a mill.api.Task[T] and unwraps it if so.
 */
def unwrapMillTask(tpe: ScType): ScType =
  val dealiasedType = tpe.removeAliasDefinitions()

  val hierarchyTypes = BaseTypes.get(dealiasedType)

  val taskTypeArgOpt = hierarchyTypes.iterator.collectFirst:
    case ParameterizedType(designator, Seq(typeArg))
      if designator.extractClass.exists(_.getQualifiedName == "mill.api.Task") =>
      typeArg

  taskTypeArgOpt.getOrElse(tpe)
end unwrapMillTask

/**
 * Checks if the type is a mill.api.Task[T] and unwraps it if so.
 */
def unwrapSeq(tpe: ScType): Option[ScType] =
  val dealiasedType = tpe.removeAliasDefinitions()

  // Если tpe === Seq, то его может не быть в списке предков
  val hierarchyTypes = tpe +: BaseTypes.get(dealiasedType)

  val taskTypeArgOpt = hierarchyTypes.iterator.collectFirst:
    case ParameterizedType(designator, Seq(typeArg))
      if designator.extractClass.exists(_.getQualifiedName == "scala.collection.immutable.Seq") =>
        typeArg

  taskTypeArgOpt
end unwrapSeq

def getParameterlessMembers(template: ScTemplateDefinition): Seq[ScTypedDefinition] =
  val ignoredBaseClasses = Set(
    "java.lang.Object",
    "scala.Any",
    "scala.AnyRef",
    "scala.AnyVal",
    "scala.Product",
    "scala.Equals"
  )

  // 1. Get the properties defined in the body { ... }
  val bodyElements: Seq[ScTypedDefinition] =
    template.allMethods.toList
      .map(_.method)
      .filterNot(method =>
        Option(method.getContainingClass).exists { clazz =>
          ignoredBaseClasses.contains(clazz.getQualifiedName)
        }
      )
      .flatMap://TODO support for java methods
        case fn: ScFunction if fn.parameters.isEmpty =>
          Seq(fn)
        case v: ScValue =>
          v.declaredElements
        case v: ScVariable =>
          v.declaredElements
        case _ =>
          Seq.empty

  val constructorParams: Seq[ScTypedDefinition] = template match
    case clazz: ScClass =>
      clazz.parameters
    case _ =>
      Seq.empty

  bodyElements ++ constructorParams
end getParameterlessMembers

def parameterLessMembersOfTheScope : CurrentScope[ScType] => List[ScTypedDefinition] =
  case CurrentScope.ObjectDefinition(extendList, _) =>
    extendList
      .flatMap(extractTemplateDefinition)
      .flatMap(getParameterlessMembers)
  case CurrentScope.OverrideRightHandSide(_, _, expectedType, _) =>
    extractTemplateDefinition(expectedType)
      .toList
      .flatMap(getParameterlessMembers)
end parameterLessMembersOfTheScope

def scopeMembersSuggestions(scope : CurrentScope[ScType]): Seq[(String, ScType)] =
  parameterLessMembersOfTheScope(scope)
    .flatMap(
      typedDef =>
        typedDef.`type`().toOption.map(unwrapMillTask).map((typedDef.name, _))
    )
end scopeMembersSuggestions

def getScalaVersion(element: YAMLPsiElement): Option[String] =
  @tailrec
  def find(current: Option[PsiElement]): Option[String] =
    current match
      case None => None
      case Some(_: YAMLDocument) => None
      case Some(kv: YAMLKeyValue) if kv.getKeyText.startsWith("object ") => None
      case Some(mapping: YAMLMapping) =>
        val scalaVersionOpt = mapping.getKeyValues.asScala
          .find(_.getKeyText == "scalaVersion")
          .flatMap(kv => Option(kv.getValue))
          .collectFirst { case scalar: YAMLScalar => scalar.getTextValue }

        scalaVersionOpt match
          case Some(version) => Some(version)
          case None => find(Option(mapping.getParent))
      case Some(other) => find(Option(other.getParent))
  end find
  find(Some(element))
end getScalaVersion
