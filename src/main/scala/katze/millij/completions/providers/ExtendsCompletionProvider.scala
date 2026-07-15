package katze.millij.completions.providers

import com.intellij.codeInsight.completion.{CompletionParameters, CompletionResultSet, CompletionUtilCore, PrefixMatcher}
import com.intellij.codeInsight.lookup.{LookupElement, LookupElementBuilder}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Iconable
import com.intellij.util.ProcessingContext
import katze.millij
import katze.millij.completions.cool.CoolCompletionProvider
import katze.millij.completions.insert
import katze.millij.completions.insert.{ExtendsArrayInsertHandler, YamlKeyInsertHandler}
import katze.millij.data.module.NamespacedPath
import katze.millij.data.{ScalaIdentifier, Smart}
import katze.millij.place.enclosingModule
import katze.millij.psi.*
import katze.millij.scalatypes.{searchForDependentOverridableTraits, searchForOverridableTraits, shortFqn}
import org.jetbrains.yaml.psi.*

import scala.jdk.CollectionConverters.*
import cats.syntax.all.*

class YamlPrefixMatcher(
  prefix: String,
  originalMatcher: PrefixMatcher,
  enclosingModule: NamespacedPath[List, ScalaIdentifier]
) extends PrefixMatcher(prefix):

  private val scopes: List[String] =
    (enclosingModule.fullPath :: enclosingModule.searchScopes).toList.map(_.asQualified)

  override def prefixMatches(name: String): Boolean =
    originalMatcher.prefixMatches(name) ||
      name.split("[.:/]+").exists(part => originalMatcher.cloneWithPrefix(prefix).prefixMatches(part)) ||
      scopes.exists { scope =>
        if scope.nonEmpty then
          val prefixStr = scope + "."
          name.startsWith(prefixStr) && originalMatcher.cloneWithPrefix(prefix).prefixMatches(name.substring(prefixStr.length))
        else
          false
      }

  override def cloneWithPrefix(newPrefix: String): PrefixMatcher =
    new YamlPrefixMatcher(newPrefix, originalMatcher.cloneWithPrefix(newPrefix), enclosingModule)
end YamlPrefixMatcher

/**
 * Adds Scala completions for extends block with single trait to implement.
 */
def scalaExtendsValueCompletionProvider(using Smart) : CoolCompletionProvider[
  CompletionPosition,
  (YAMLScalar, YAMLKeyValueWithKey["extends"], YAMLMapping, YAMLMillModule)
] =
  case (
    parameters: CompletionParameters,
    element: CompletionPosition,
    (scalar, _, _, _),
    context : ProcessingContext,
    resultSet: CompletionResultSet
  ) =>
    val rawPrefix = scalar.getTextValue
    val cleanPrefix = rawPrefix.replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "")
    val customResultSet = resultSet.withPrefixMatcher(cleanPrefix)
    makeScalaExtendsSuggestions(element.getProject, Nil)
      .foreach(customResultSet.addElement)
end scalaExtendsValueCompletionProvider

/**
 * Adds YAML completions for extends block with single trait to implement.
 */
def yamlExtendsValueCompletionProvider(using Smart) : CoolCompletionProvider[
  CompletionPosition,
  (YAMLScalar, YAMLKeyValueWithKey["extends"], YAMLMapping, YAMLMillModule)
] =
  case (
    parameters: CompletionParameters,
    element: CompletionPosition,
    (scalar, _, _, _),
    context : ProcessingContext,
    resultSet: CompletionResultSet
  ) =>
    val rawPrefix = scalar.getTextValue
    val cleanPrefix = rawPrefix.replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "")
    enclosingModule(scalar).foreach(enclosingModule =>
      val customMatcher = new YamlPrefixMatcher(cleanPrefix, resultSet.getPrefixMatcher, enclosingModule)
      val customResultSet = resultSet.withPrefixMatcher(customMatcher)
      makeYamlExtendsSuggestions(element.getProject, enclosingModule, Nil)
        .foreach(customResultSet.addElement)
    )
end yamlExtendsValueCompletionProvider

/**
 * Adds Scala completions for extends block with list of traits to implement.
 */
def scalaExtendsListCompletionProvider(using Smart) : CoolCompletionProvider[
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
    val rawPrefix = scalar.getTextValue
    val cleanPrefix = rawPrefix.replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "")
    val customResultSet = resultSet.withPrefixMatcher(cleanPrefix)
    val alreadyPresent = sequence
        .getItems.asScala
        .map(_.getValue)
        .collect { case s : YAMLScalar => s.getTextValue }
        .toList
    makeScalaExtendsSuggestions(element.getProject, alreadyPresent)
      .foreach(customResultSet.addElement)
end scalaExtendsListCompletionProvider

/**
 * Adds YAML completions for extends block with list of traits to implement.
 */
def yamlExtendsListCompletionProvider(using Smart) : CoolCompletionProvider[
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
    val rawPrefix = scalar.getTextValue
    val cleanPrefix = rawPrefix.replace(CompletionUtilCore.DUMMY_IDENTIFIER_TRIMMED, "")
    val alreadyPresent = sequence
        .getItems.asScala
        .map(_.getValue)
        .collect { case s : YAMLScalar => s.getTextValue }
        .toList
    enclosingModule(scalar).foreach(enclosingModule =>
      val customMatcher = new YamlPrefixMatcher(cleanPrefix, resultSet.getPrefixMatcher, enclosingModule)
      val customResultSet = resultSet.withPrefixMatcher(customMatcher)
      makeYamlExtendsSuggestions(element.getProject, enclosingModule, alreadyPresent)
        .foreach(customResultSet.addElement)
    )
end yamlExtendsListCompletionProvider

/**
 * Finds all the traits that are Modules and excludes those that the module already extends.
 * @param exclude Already extended classes.
 * @return
 */
def makeScalaExtendsSuggestions(project : Project, exclude : List[String])(using Smart) : List[LookupElement] =
  val scalaTraits = searchForOverridableTraits(project)
    .toList
    .flatten
    .filter(clazz => clazz.getQualifiedName != null && clazz.getName != null)
    .filter(clazz => !exclude.contains(clazz.getQualifiedName))
    .map(clazz =>
        LookupElementBuilder
          .create(shortFqn(clazz).asQualified)
          .withLookupString(clazz.getName)
          .withPresentableText(clazz.getName)
          .withIcon(clazz.getIcon(Iconable.ICON_FLAG_VISIBILITY))
          .withTypeText(clazz.getQualifiedName)
    )

  scalaTraits
end makeScalaExtendsSuggestions

/**
 * Finds all the members of other modules that are mill Modules and excludes those that the module already being extended from.
 * @param exclude Already extended classes.
 * @return
 */
def makeYamlExtendsSuggestions(
  project : Project,
  module: NamespacedPath[List, ScalaIdentifier],
  exclude : List[String]
)(using Smart) : List[LookupElement] =
  val yamlTraits = searchForDependentOverridableTraits(module, project)
    .map((path, _, tie) => (path.asQualified, tie))
    .filter((name, _) => !exclude.contains(name))
    .map((name, tie) =>
      LookupElementBuilder
        .create(name)
        .withIcon(tie.getIcon(Iconable.ICON_FLAG_VISIBILITY))
        .withTypeText(tie.getQualifiedName)
    )

  yamlTraits
end makeYamlExtendsSuggestions

/**
 * Adds completions for extends keyword when it is being typed inside existing key-value pair
 * 
 * ```YAML
 * object moduleA:
 *  ext<caret>:
 * ```
 * 
 * Note that it is fired only when there is : after the caret.
 */
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
      .withInsertHandler(YamlKeyInsertHandler)

    resultSet.addElement(
      LookupElementBuilder
        .create("extends")
        .withPresentableText("extends: []")
        .withInsertHandler(ExtendsArrayInsertHandler)
    )

    resultSet.addElement(lookupElement)
end extendsExistingKeyCompletionProvider

/**
 * Adds completions for extends keyword when it is being typed in module body
 *
 * ```YAML
 * object moduleA:
 *  ext<caret>
 * ```
 * 
 * Note that it is not fired when there is : after the caret.
 */
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
