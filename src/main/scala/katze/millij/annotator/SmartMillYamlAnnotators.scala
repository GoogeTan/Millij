package katze.millij.annotator

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiElement
import katze.millij.annotator.{CoolAnnotatorAdapter, mvnDepsAnnotator}
import katze.millij.cool.{CoolPattern, PsiElementMatcher, PsiParentElementMatcher}
import katze.millij.data.MillijBundle
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
      extendsBlockMembersAnnotator(
        (enclosingModule, scalar, annotationHolder, isOnCycle) =>
          if isOnCycle then
            annotationHolder
              .newAnnotation(HighlightSeverity.ERROR, MillijBundle.message("cycled.dependencies.error"))
              .range(scalar)
              .create()
          else if !isValidExtendsBlockMember(enclosingModule, scalar) then
            annotationHolder
              .newAnnotation(HighlightSeverity.ERROR, MillijBundle.message("unexisting.trait.name.error", scalar.getTextValue))
              .range(scalar)
              .create()
      ),
      CoolPattern.elementAndParents[
        YAMLKeyValueWithKey["extends"],
        (
          YAMLMapping,
            YAMLMillModule
          )
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
