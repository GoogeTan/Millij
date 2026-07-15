package katze.millij.place

import cats.{Applicative, Id, Monad}
import cats.syntax.all.*
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import katze.millij.data.*
import katze.millij.data.module.NamespacedPath
import katze.millij.file.*
import katze.millij.place.PlaceConfigResolver.EitherString
import katze.millij.psi.*
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.project.ProjectContext
import org.jetbrains.yaml.psi.*

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
 * Extracts and returns the object name if the text is a correct object declaration.
 * Returns Some(name) if matched, or None otherwise.
 */
def extractObjectName(text: String): Option[ScalaIdentifier] =
  // 1. Define the pattern as a String first (interpolating a Regex object calls .toString anyway)
  val objectNamePattern = "[a-zA-Z_$][\\w$]*"

  // 2. Wrap the pattern in parentheses () to create a capture group.
  // Note: We use $$ to represent the literal end-of-string $ character in an interpolated string.
  val objectRegex: Regex = s"object ($objectNamePattern)$$".r

  // 3. Use pattern matching to extract the captured group
  text match
    case objectRegex(name) => Some(ScalaIdentifier.unsafe(name))
    case _                 => None
end extractObjectName

/**
 * Returns true if text is a correct text of key value that defines an extends block of a module.
 * @param text
 * @return
 */
def isExtendsBlock(text : String) : Boolean =
  text.matches("extends")
end isExtendsBlock

def isMemberName(text : String) : Boolean =
  !isExtendsBlock(text) && !isObjectDeclarationText(text)
end isMemberName

def magicFold[F[_] : Monad as M, A, B](initial : A, list : List[B])(f : (A, List[B]) => F[(A, List[B])]) : F[A] =
  M.iterateWhileM((initial, list))(f(_, _))(_._2.nonEmpty)
    .map(_._1)
end magicFold

def placeFolder[F[_] : Monad, Place](
  resolver : YAMLConfigResolver[F, Place],
  theWholeSequence : List[YAMLPsiElement]
) : (Place, List[YAMLPsiElement]) => F[(Place, List[YAMLPsiElement])] =
  case (currentPlace, (mapping: YAMLMapping) :: tail) =>
    resolver.mapping(currentPlace, mapping).map((_, tail))
  case (currentPlace, (kv: YAMLKeyValue) :: (body: YAMLMapping) :: tail) =>
    extractObjectName(kv.getKeyText) match
      case Some(value) =>
        resolver.module(currentPlace, value, body).map((_, tail))
      case None =>
        for
          withField <- resolver.field(currentPlace, kv.getKeyText)
          res <- resolver.mapping(withField, body)
        yield (res, tail)
    end match
  case (currentPlace, (kv: YAMLKeyValue) :: elem :: _) if kv.getKey == elem =>
    (currentPlace, Nil).pure
  case (currentPlace, (kv: YAMLKeyValue) :: Nil) =>
    (currentPlace, Nil).pure
  case (currentPlace, (kv: YAMLKeyValue) :: tail) =>
    resolver.field(currentPlace, kv.getKeyText).map((_, tail))
  case (currentPlace, (_: YAMLSequence) :: Nil) =>
    (currentPlace, Nil).pure
  case (currentPlace, (_: YAMLSequence) :: (_: YAMLSequenceItem) :: Nil) =>
    (currentPlace, Nil).pure
  case (currentPlace, (_: YAMLSequence) :: (_: YAMLSequenceItem) :: tail) =>
    resolver.sequenceItem(currentPlace).map((_, tail))
  case (currentPlace, (_: YAMLScalar) :: Nil) =>
    (currentPlace, Nil).pure
  case (_, elem :: tail) =>
    resolver.onUnexpected(theWholeSequence.takeWhile(_ != elem) :+ elem).map((_, Nil))
  case (currentPlace, Nil) =>
    (currentPlace, Nil).pure
end placeFolder

/**
 * Tries to resolve a place of an element in a YAML mill config.
 * It goes up to the document root and then builds position down.
 *
 * TODO make it tailrec by collecrint the whole path to the list and then by folding it.
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
  val untypedParents =
    PsiChild.unapplySeq(element)
      .takeWhile(parent => !parent.isInstanceOf[YAMLDocument] && !parent.isInstanceOf[YAMLFile])
      .reverse
  val parents = untypedParents.collect { case yaml: YAMLPsiElement => yaml }.toList
  assert(parents.length == untypedParents.length)
  parents match
    case (head: YAMLMapping) :: tail =>
      for
        topLevelModule <- resolver.topLevelModule(head)
        result <- magicFold(topLevelModule, tail)(placeFolder(resolver, parents))
      yield result
    case anythingElse =>
      resolver.onUnexpected(parents)
end placeOf

def richPlaceOf(
  currentElement : YAMLPsiElement,
)(using Smart): Either[String, PlaceInYamlConfig[ScType]] =
  val project = currentElement.getProject
  elementsFilePath(currentElement).map(
    richPlaceConfigResolver(project, _)
  ).flatMap(placeOf(currentElement, _))
end richPlaceOf

/**
 * Relative location of a file to which element belongs.
 *
 * It is the same as a name of root module in a file in which element is.
 *
 * @param element Relative path or error.
 * @return
 */
def elementsFilePath(element : PsiElement) : Either[String, SegmentedPath[List, ScalaIdentifier]] =
  val filePathRelativeToProjectRoot = element
    .getContainingFile.relativePathToContentRoot.toRight("Couldn't determine a file location relative to the project root")
  filePathRelativeToProjectRoot
    .map(SegmentedPath.folderPath)
    .flatMap(_.traverse(ScalaIdentifier.fromStringEither))
end elementsFilePath

def richPlaceConfigResolver(
  project : Project,
  relativePath : SegmentedPath[List, ScalaIdentifier]
)(using Smart): PlaceConfigResolver[EitherString] =
  PlaceConfigResolver(relativePath, richPlaceSearch(project))
end richPlaceConfigResolver


def richPlaceConfigResolverOption(
  project : Project,
  relativePath : SegmentedPath[List, ScalaIdentifier]
)(using Smart): PlaceConfigResolver[Option] =
  PlaceConfigResolver.option(relativePath, richPlaceSearch[Id](project))
end richPlaceConfigResolverOption


def richPlaceSearch[F[_] : Applicative](project : Project)(using Smart) = (module: NamespacedPath[List, ScalaIdentifier], name: String) =>
  given ProjectContext = ProjectContext.fromProject(project)
  SegmentedPath
    .fromQualifiedNonEmpty(name)
    .flatMap(_.traverse(ScalaIdentifier.fromStringOption))
    .flatMap(
      project.getService(classOf[MillModuleService])
        .resolvePath(module, _)
        .completelyResolvedTarget
        .flatMap(_.scalaTraitType)
    )
    .pure[F]
end richPlaceSearch