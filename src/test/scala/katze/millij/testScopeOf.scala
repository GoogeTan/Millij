package katze.millij

import cats.data.*
import katze.millij.place.{YAMLConfigResolver, placeOf}
import katze.millij.psi.CompletionPosition
import org.jetbrains.yaml.psi.{YAMLMapping, YAMLScalar, YAMLSequence}

import scala.jdk.CollectionConverters.*
import cats.syntax.all.*
import com.intellij.psi.PsiElement
import katze.millij.TestType
import org.jetbrains.yaml.psi.YAMLPsiElement

def testScopeOf(element: CompletionPosition): Either[Chain[String], TestType] =
  type F[A] = Either[Chain[String], A]
  val resolver = new YAMLConfigResolver[F, TestType]:
    override def field(parent: TestType, name: String): F[TestType] = Right(TestType.MemberOf(parent, name))

    override def module(parent: TestType, name: String, mapping: YAMLMapping): F[TestType] =
      if name.startsWith("object ") then
        extendsOf(mapping)
          .leftMap(_.map(_.toString))
          .map(opt => TestType.ClassFromName(opt.getOrElse(List.empty)))
      else
        Right(TestType.MemberOf(parent, name))

    override def sequenceItem(parent: TestType): F[TestType] = Right(TestType.UnSeqOf(parent))

    override def topLevelModule(mapping: YAMLMapping): F[TestType] =
      extendsOf(mapping)
        .leftMap(_.map(_.toString))
        .map(opt => TestType.ClassFromName(opt.getOrElse(List.empty)))

    override def mapping(parent: TestType, mapping: YAMLMapping): F[TestType] = Right(parent)

    override def onUnexpected(element: YAMLPsiElement): F[TestType] = Left(Chain(s"Got unexpected $element"))
  val parent = assertMatches(element.getParent,
    {
      case yaml: YAMLPsiElement => Some(yaml)
      case _ => None
    }
  )
  placeOf(parent, resolver)
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