package katze.millij

import cats.Monad
import cats.syntax.all.*
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import katze.millij.scalatypes.classTypeSearch
import katze.millij.psi.{CompletionPosition, YAMLChild, YAMLGrandChild}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.yaml.psi.*

import scala.jdk.CollectionConverters.*
import scala.util.matching.Regex

val objectNameRegex: Regex = "[a-zA-Z_$][\\w$]*$".r

val objectRegex = s"object $objectNameRegex".r

def isObjectDeclarationText(text : String) : Boolean =
  objectRegex.matches(text)
end isObjectDeclarationText

def isExtendsBlock(text : String) : Boolean =
  text.matches("extends")
end isExtendsBlock

def scopeOf[F[_] : Monad, Scope](
  element: YAMLPsiElement | CompletionPosition,
  fieldScope: (Scope, String) => F[Scope],
  objectScope: (Scope, String, YAMLMapping) => F[Scope],
  sequenceScope: Scope => F[Scope],
  extendsOf: YAMLMapping => F[Scope],
  mappingScope : (Scope, YAMLMapping) => F[Scope],
  somethingElse: PsiElement => F[Scope]
): F[Scope] =
  element match
    case YAMLChild(kv: YAMLScalar, mapping: YAMLMapping) =>
      scopeOf(mapping, fieldScope, objectScope, sequenceScope, extendsOf, mappingScope, somethingElse)
        .flatMap(mappingScope(_, mapping))
    case YAMLGrandChild(self, kv: YAMLKeyValue, mapping: YAMLMapping) =>
      self match
        case _ if kv.getKey == self =>
          scopeOf(mapping, fieldScope, objectScope, sequenceScope, extendsOf, mappingScope, somethingElse)
            .flatMap(mappingScope(_, mapping))
        case self: YAMLMapping if kv.getValue == self && isObjectDeclarationText(kv.getKeyText) =>
          scopeOf(mapping, fieldScope, objectScope, sequenceScope, extendsOf, mappingScope, somethingElse)
            .flatMap(
              objectScope(_, kv.getKeyText, self)
            )
            .flatMap(mappingScope(_, mapping))
        case self if kv.getValue == self =>
          scopeOf(mapping, fieldScope, objectScope, sequenceScope, extendsOf, mappingScope, somethingElse)
            .flatMap(
              fieldScope(_, kv.getKeyText)
            )
            .flatMap(mappingScope(_, mapping))
        case el =>
          somethingElse(el)
      end match
    case YAMLChild(kv: YAMLKeyValue, mapping: YAMLMapping) =>
      scopeOf(mapping, fieldScope, objectScope, sequenceScope, extendsOf, mappingScope, somethingElse)
    case YAMLGrandChild(self, _: YAMLSequenceItem, seq: YAMLSequence) =>
      scopeOf(seq, fieldScope, objectScope, sequenceScope, extendsOf, mappingScope, somethingElse).flatMap(sequenceScope)
    case YAMLGrandChild(self, parent: YAMLMapping, document: YAMLDocument) =>
      extendsOf(parent)

    case YAMLChild(m: YAMLMapping, parent: YAMLDocument) =>
      extendsOf(m)

    case YAMLChild(CompletionPosition(self), parent: YAMLPsiElement) =>
      scopeOf(parent, fieldScope, objectScope, sequenceScope, extendsOf, mappingScope, somethingElse)

    case elseElement =>
      somethingElse(elseElement)
  end match
end scopeOf

def richScopeOf(
  currentElement : YAMLPsiElement | CompletionPosition,
): Either[String, CurrentScope[ScType]] =
  val project = currentElement.getProject
  val search = (text :String) =>
    Right(classTypeSearch(ScalaPsiManager.instance(project), GlobalSearchScope.allScope(project), text))
  scopeOf[[T] =>> Either[String, T], CurrentScope[ScType]](
    element = currentElement,
    fieldScope = (scope, field) => fieldScope(scope, field).toRight(s"Couldn't find field $field in scope $scope"),
    objectScope = objectScope(
      _,
      _,
      _,
      search,
      scope => Left(s"Expected object body but got method right hand side ${scope.name} with type ${scope.expectedType}")
    ),
    sequenceScope = scope => sequenceScope[[T] =>> Either[String, T]](
      scope,
      Left(s"Expected RHS but got object $scope"),
      Left(s"Expected sequence scope but got $scope"),
    ),
    extendsOf = extendsOf[[X] =>> Either[String, X], ScType](_, search),
    mappingScope = (scope, mapping) =>
      scope match
        case CurrentScope.ObjectDefinition(extendList, definedMembers) =>
          CurrentScope.ObjectDefinition(
            extendList = extendList,
            definedMembers = mapping.getKeyValues.asScala.map(_.getKeyText).toList
          ).pure
        case CurrentScope.OverrideRightHandSide(parentTypes, name, expectedType, definedMembers) =>
          CurrentScope.OverrideRightHandSide(
            parentTypes = parentTypes,
            name = name,
            expectedType = expectedType,
            definedMembers = mapping.getKeyValues.asScala.map(_.getKeyText).toList
          ).pure,
    somethingElse = got => Left(s"Couldn't parse PSI. Got $got of class ${got.getClass} with parent ${got.getParent}")
  )
end richScopeOf
