package katze.millij.annotator

import cats.syntax.all.*
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.text.HtmlChunk
import com.intellij.psi.PsiElement
import katze.millij.annotator.{Annotators, CoolAnnotatorAdapter, methodAndFieldAnnotator, mvnDepsAnnotator}
import katze.millij.cool.{CoolPattern, PsiElementMatcher, PsiParentElementMatcher}
import katze.millij.place.{PlaceInYamlConfig, richScopeOf, yamlDefinableMembersOfScope}
import katze.millij.psi.*
import org.jetbrains.plugins.scala.lang.psi.types.{ScType, ScTypeExt, TypePresentationContext}
import org.jetbrains.yaml.psi.{YAMLKeyValue, YAMLMapping, YAMLScalar, YAMLSequence, YAMLSequenceItem}

final class MillYamlAnnotator extends Annotators(
  List(
    CoolAnnotatorAdapter(
      methodAndFieldAnnotator,
      CoolPattern.elementAndParent()
    ),
    CoolAnnotatorAdapter(
      unexistingMembersAnnotator(unexistingMembersError),
      CoolPattern.elementAndParents[YAMLKey[PsiElement], (YAMLKeyValue, YAMLMapping)]()
    ),
    CoolAnnotatorAdapter(
      mvnDepsAnnotator,
      CoolPattern.element
    ),
    CoolAnnotatorAdapter(
      extendsListBlockAnnotator(isValidExtendsBlockMember),
      CoolPattern.elementAndParent[
        YAMLScalar,
        Either[
          (YAMLSequenceItem, YAMLSequence, YAMLKeyValueWithKey["extends"]),
          YAMLKeyValueWithKey["extends"]
        ]
      ]()
    ),
    CoolAnnotatorAdapter(
      objectInInappropriatePlace,
      CoolPattern.elementAndParent()
    ),
  )
)

def unexistingMembersError(mapping : YAMLMapping, kv : YAMLKeyValue) : Option[String] =
  richScopeOf(mapping).toOption.flatMap(scope =>
    val possibleMembers = yamlDefinableMembersOfScope(scope)
    if possibleMembers.exists(_.getName() === kv.getKeyText) then
      None
    else
      given context: TypePresentationContext = TypePresentationContext(kv)

      val scopeText = scope match
        case PlaceInYamlConfig.Module(extendList, definedMembers) =>
          "module that extends " + extendList.map(typeReference).mkString(", ")
        case PlaceInYamlConfig.Member(parentTypes, name, expectedType, definedMembers) =>
          s"object of type ${typeReference(expectedType)}</a>"

      Some(
        s"<html><body>Couldn't find member with name '${kv.getKeyText}' to override in ${scopeText} </body>.</html>"
      )
  )
end unexistingMembersError

def typeReference(scType : ScType)(using TypePresentationContext) : String =
  //TODO fix go to definition
  val cleanFqn = scType.extractClass match
    case Some(psiClass) => psiClass.getQualifiedName
    case None => scType.canonicalText.takeWhile(_ != '[')
  HtmlChunk.link(s"psi_element://$cleanFqn", scType.presentableText).toString
end typeReference

//TODO rename me
def objectTextRanges(textRange : TextRange) : (TextRange, TextRange) =
  val objectText = "object"
  val objectKeywordRange = TextRange.create(textRange.getStartOffset, textRange.getStartOffset + objectText.length)
  val nameRange = TextRange.create(textRange.getStartOffset + objectText.length, textRange.getEndOffset)
  (objectKeywordRange, nameRange)
end objectTextRanges
