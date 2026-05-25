package katze.millij

import cats.data.*
import katze.millij.scopeOf
import katze.millij.psi.CompletionPosition
import org.jetbrains.yaml.psi.{YAMLMapping, YAMLScalar, YAMLSequence}

import scala.jdk.CollectionConverters.*
import cats.syntax.all.*
import com.intellij.psi.PsiElement
import katze.millij.TestType

def testScopeOf(element: CompletionPosition): Either[Chain[String], TestType] =
  scopeOf[[T] =>> Either[Chain[String], T], TestType](
    element = element,
    fieldScope = (original, field) => Right(TestType.MemberOf(original, field)),
    objectScope = (original, name, psi) =>
      if name.startsWith("object ") then
        extendsOf(psi)
          .leftMap(_.map(_.toString))
          .map(opt => TestType.ClassFromName(opt.getOrElse(List.empty)))
      else
        Right(TestType.MemberOf(original, name)),
    sequenceScope = original => Right(TestType.UnSeqOf(original)),
    extendsOf = mapping =>
      extendsOf(mapping)
        .leftMap(_.map(_.toString))
        .map(opt => TestType.ClassFromName(opt.getOrElse(List.empty))),
    mappingScope = (original, _) => Right(original),
    somethingElse = element =>
      Left(Chain(s"Got unexpected $element"))
  )
end testScopeOf

enum ExtendsBeingMalformated:
  case WrongBodyElement(elementFound : PsiElement)
  case WrongSequenceElement(elementFound : PsiElement)
  case WrongValueElement(elementFound : PsiElement)
end ExtendsBeingMalformated

def extendsOf(
  psi: YAMLMapping
): Either[Chain[ExtendsBeingMalformated], Option[List[String]]] =
  val element = psi.getKeyValueByKey("extends")
  if element == null then
    Right(None)
  else
    element.getValue match
      case sequence: YAMLSequence =>
        val (malformated, formated) = sequence.getItems.asScala.toList.map(_.getValue).partitionMap:
          case scalar: YAMLScalar => Right(scalar.getTextValue)
          case another => Left(ExtendsBeingMalformated.WrongSequenceElement(another))

        if malformated.nonEmpty then
          Left(Chain.fromSeq(malformated))
        else
          Right(Some(formated))
      case scalar: YAMLScalar =>
        Right(Some(List(scalar.getTextValue)))
      case null =>
        Right(Some(List()))
      case found =>
        Left(
          Chain(
            ExtendsBeingMalformated.WrongValueElement(found)
          )
        )
    end match
  end if
end extendsOf
