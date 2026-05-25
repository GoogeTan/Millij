package katze.millij.completions.providers

import com.intellij.codeInsight.completion.{CompletionParameters, CompletionResultSet}
import com.intellij.codeInsight.lookup.{LookupElement, LookupElementBuilder}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.util.ProcessingContext
import katze.millij
import katze.millij.completions.cool.CoolCompletionProvider
import katze.millij.completions.insert
import katze.millij.completions.insert.{ExtendsArrayInsertHandler, YamlKeyInsertHandler}
import katze.millij.scalatypes.searchForOverridableTraits
import katze.millij.psi.*
import katze.millij.place.isExtendsBlock
import org.jetbrains.yaml.psi.*

import scala.jdk.CollectionConverters.*

def extendsValueCompletionProvider : CoolCompletionProvider[
  CompletionPosition,
  YAMLScalar *: YAMLKeyValue *: YAMLMapping *: YAMLMillModule *: EmptyTuple
] =
  case (
    parameters: CompletionParameters,
    element: CompletionPosition,
    scalar *: field *: _ *: _ *: EmptyTuple,
    context : ProcessingContext,
    resultSet: CompletionResultSet
  ) =>
    if isExtendsBlock(field.getKeyText) then
      makeExtendsSuggestions(element.getProject, Nil)
        .foreach(resultSet.addElement)
    end if
end extendsValueCompletionProvider

def extendsListCompletionProvider : CoolCompletionProvider[
  CompletionPosition,
  YAMLScalar *: YAMLSequenceItem *: YAMLSequence *: YAMLKeyValueWithKey["extends"] *: YAMLMapping *: YAMLMillModule *: EmptyTuple
] =
  case (
    parameters: CompletionParameters,
    element: CompletionPosition,
    scalar *: _ *: sequence *: field *: _ *: _ *: EmptyTuple,
    context : ProcessingContext,
    resultSet: CompletionResultSet
  ) =>
    val alreadyPresent = sequence
        .getItems.asScala
        .map(_.getValue)
        .collect { case s : YAMLScalar => s.getTextValue }
        .toList
    makeExtendsSuggestions(element.getProject, alreadyPresent)
      .foreach(resultSet.addElement)
end extendsListCompletionProvider

def makeExtendsSuggestions(project : Project, exclude : List[String]) : List[LookupElement] =
  searchForOverridableTraits(project)
    .toList
    .flatten
    .filter(clazz => !exclude.contains(clazz.getQualifiedName))
    .map(clazz =>
        LookupElementBuilder
          .create(clazz.getQualifiedName)
          .withLookupString(clazz.getName)
          .withPresentableText(clazz.getName)
          .withIcon(clazz.getIcon(Iconable.ICON_FLAG_VISIBILITY))
          .withTypeText(clazz.getQualifiedName)
    )
end makeExtendsSuggestions

def extendsExistingKeyCompletionProvider : CoolCompletionProvider[
  YAMLKey[CompletionPosition],
  YAMLKeyValue *: YAMLMapping *: YAMLMillModule *: EmptyTuple
] =
  case (
    parameters: CompletionParameters,
    element: CompletionPosition,
    _,
    context : ProcessingContext,
    resultSet: CompletionResultSet
  ) =>
    val lookupElement = LookupElementBuilder
      .create("extends")
      .withPresentableText("extends")
      .withTypeText("classes to extend")
      .withInsertHandler(YamlKeyInsertHandler)//TODO add second autocompletion that adds []

    resultSet.addElement(lookupElement)
end extendsExistingKeyCompletionProvider

def extendsNewKeyCompletionProvider : CoolCompletionProvider[
  CompletionPosition,
  YAMLScalar *: YAMLMapping *: YAMLMillModule *: EmptyTuple
] =
  case (
    parameters: CompletionParameters,
    element: CompletionPosition,
    scalar *: mapping *: _ *: EmptyTuple,
    context : ProcessingContext,
    resultSet: CompletionResultSet
  ) =>
    resultSet.addElement(
      LookupElementBuilder
        .create("extends")
        .withPresentableText("extends")
        .withInsertHandler(insert.YamlKeyInsertHandler)
    )
    resultSet.addElement(
      LookupElementBuilder
        .create("extends")
        .withPresentableText("extends: []")
        .withInsertHandler(ExtendsArrayInsertHandler)
    )
end extendsNewKeyCompletionProvider
