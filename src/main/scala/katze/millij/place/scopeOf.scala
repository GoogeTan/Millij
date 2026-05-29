package katze.millij.place

import cats.Monad
import cats.syntax.all.*
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import katze.millij.data.TypeSearchCache
import katze.millij.psi.{CompletionPosition, YAMLChild, YAMLGrandChild}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.yaml.psi.*

import scala.jdk.CollectionConverters.*
import scala.util.matching.Regex

/**
 * Returns true if text is a correct text of key value that defines a module.
 */
def isObjectDeclarationText(text : String) : Boolean =
  val objectNameRegex: Regex = "[a-zA-Z_$][\\w$]*$".r

  val objectRegex = s"object $objectNameRegex".r

  objectRegex.matches(text)
end isObjectDeclarationText

/**
 * Returns true if text is a correct text of key value that defines an extends block of a module.
 * @param text
 * @return
 */
def isExtendsBlock(text : String) : Boolean =
  text.matches("extends")
end isExtendsBlock

/**
 * Tries to resolve a place of an element in a YAML mill config.
 * It goes up to the document root and then builds position down.
 *
 * @param element Element to resolve a place of
 * @param fieldPlace A place of a field inside a Place.
 * @param modulePlace Module declaration place
 * @param sequenceItemPlace Sequence item place
 * @param topLevelModulePlace Makes a place of document root
 * @param mappingPlace Place of a mapping. It possibly populated Place information about members defined in a mapping.
 * @param somethingElse Called if document is malformed and unexpected situation accured.
 * @tparam F Effect of the calculation. Possibly contains errors for malformed documents.
 */
def placeOf[F[_] : Monad, Place](
  element: YAMLPsiElement,
  resolver: YAMLConfigResolver[F, Place],
): F[Place] =
  def walkRecursively(element : YAMLPsiElement) : F[Place] =
    element match
      case YAMLChild(_: YAMLScalar, mapping: YAMLMapping) =>
        walkRecursively(mapping)
          .flatMap(resolver.mapping(_, mapping))
      case YAMLGrandChild(self, kv: YAMLKeyValue, mapping: YAMLMapping) =>
        self match
          case _ if kv.getKey == self =>
            walkRecursively(mapping)
              .flatMap(resolver.mapping(_, mapping))
          case self: YAMLMapping if kv.getValue == self && isObjectDeclarationText(kv.getKeyText) =>
            walkRecursively(mapping)
              .flatMap(
                resolver.module(_, kv.getKeyText, self)
              )
              .flatMap(resolver.mapping(_, mapping))
          case self if kv.getValue == self =>
            walkRecursively(mapping)
              .flatMap(
                resolver.field(_, kv.getKeyText)
              )
              .flatMap(resolver.mapping(_, mapping))
          case el =>
            resolver.onUnexpected(el)
        end match
        
      case YAMLChild(_: YAMLKeyValue, mapping: YAMLMapping) =>
        walkRecursively(mapping)
        
      case YAMLGrandChild(_, _: YAMLSequenceItem, seq: YAMLSequence) =>
        walkRecursively(seq)
          .flatMap(resolver.sequenceItem)
        
      case YAMLGrandChild(_, parent: YAMLMapping, _: YAMLDocument) =>
        resolver.topLevelModule(parent)
        
      case YAMLChild(moduleBody: YAMLMapping, _: YAMLDocument) =>
        resolver.topLevelModule(moduleBody)
        
      case elseElement =>
        resolver.onUnexpected(elseElement)
    end match
  end walkRecursively
  
  element match
    case YAMLChild(CompletionPosition(_), parent: YAMLPsiElement) =>
      walkRecursively(parent)
    case element: YAMLPsiElement =>
      walkRecursively(element)
    case unexpectedElement =>
      resolver.onUnexpected(unexpectedElement)
end placeOf

def richScopeOf(
  currentElement : YAMLPsiElement,
): Either[String, PlaceInYamlConfig[ScType]] =
  val project = currentElement.getProject
  val search = (text : String) => 
    Right(project.getService(classOf[TypeSearchCache]).search(text))
    
  placeOf(
    currentElement,
    PlaceConfigResolver(search)
  )
end richScopeOf
