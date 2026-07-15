package katze.millij.place

import cats.data.*
import cats.syntax.all.*
import com.intellij.psi.PsiElement
import katze.millij.place.TestPlace
import katze.millij.assertMatches
import katze.millij.place.{YAMLConfigResolver, placeOf, TestPlaceYAMLConfigResolver}
import katze.millij.psi.CompletionPosition
import org.jetbrains.yaml.psi.{YAMLMapping, YAMLPsiElement, YAMLScalar, YAMLSequence}

import scala.jdk.CollectionConverters.*

def testPlaceOf(element: CompletionPosition): Either[Chain[String], TestPlace] =
  type F[A] = Either[Chain[String], A]
  
  val parent = assertMatches(element.getParent,
    {
      case yaml: YAMLPsiElement => Some(yaml)
      case _ => None
    }
  )
  placeOf(parent, TestPlaceYAMLConfigResolver)
end testPlaceOf

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