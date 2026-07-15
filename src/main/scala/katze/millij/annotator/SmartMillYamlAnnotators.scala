package katze.millij.annotator

import com.intellij.psi.PsiElement
import katze.millij.annotator.{CoolAnnotatorAdapter, mvnDepsAnnotator}
import katze.millij.cool.{CoolPattern, PsiElementMatcher, PsiParentElementMatcher}
import katze.millij.place.*
import katze.millij.psi.*
import org.jetbrains.yaml.psi.*

final class SmartMillYamlAnnotators extends SmartAnnotators(
  List(
    CoolAnnotatorAdapter(
      unexistingMembersAnnotator(unexistingMembersError),
      CoolPattern.elementAndParents[YAMLKey[PsiElement], (YAMLKeyValue, YAMLMapping)]()
    ),
    CoolAnnotatorAdapter(
      extendsListBlockAnnotator(element =>
        enclosingModule(element).flatMap(enclosingModule =>
          Option
            .when(!isValidExtendsBlockMember(enclosingModule, element))(
              unexistingTraitNameError(element.getTextValue)
            )
        )
      ),
      CoolPattern.elementAndParent[
        YAMLScalar,
        Either[
          (YAMLSequenceItem, YAMLSequence, YAMLKeyValueWithKey["extends"]),
          YAMLKeyValueWithKey["extends"]
        ]
      ]()
    ),
    CoolAnnotatorAdapter(
      mvnDepsAnnotator,
      CoolPattern.elementAndParent()
    ),
    CoolAnnotatorAdapter(
      methodAndFieldAnnotator(element =>
        richPlaceOf(element).toOption.collect:
          case PlaceInYamlConfig.Member(_, expectedType, _) if !isMvnDependency(expectedType) =>
            false
          case PlaceInYamlConfig.Module(_, _, _) =>
            true
      ),
      CoolPattern.elementAndParent()
    ),
  )
)
