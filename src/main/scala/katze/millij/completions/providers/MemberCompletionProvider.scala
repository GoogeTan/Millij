package katze.millij.completions.providers

import cats.syntax.all.*
import com.intellij.codeInsight.completion.{CompletionParameters, CompletionResultSet}
import com.intellij.codeInsight.lookup.{LookupElement, LookupElementBuilder}
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.ProcessingContext
import katze.millij.completions.cool.CoolCompletionProvider
import katze.millij.psi.CompletionPosition
import katze.millij.place.{yamlDefinableMembersOfScope, richScopeOf}
import katze.millij.scalatypes.unwrapMillTask
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil.inNameContext
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScTypedDefinition
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScMember, ScTemplateDefinition}
import org.jetbrains.plugins.scala.lang.psi.types.result.Failure
import org.jetbrains.yaml.psi.YAMLPsiElement

/**
 * Adds completions for module members and object params.
 */
def memberCompletionProvider(logger : Logger) : CoolCompletionProvider[CompletionPosition, YAMLPsiElement] =
  case (
    parameters: CompletionParameters,
    psiElement: CompletionPosition,
    yamlElement,
    context: ProcessingContext,
    resultSet: CompletionResultSet
  ) =>
    richScopeOf(yamlElement) match
      case Left(errValue) =>
        logger.debug(s"Couldn't build completions for $psiElement of class ${psiElement.getClass.getSimpleName}:\n ${errValue}")
      case Right(scope) =>
        yamlDefinableMembersOfScope(scope)
          .filter(member => !scope.definedMembers.contains(member.name))
          .map(makeLookupElementForTypedDefinition(_, logger))
          .foreach(resultSet.addElement)
    end match
end memberCompletionProvider

def makeLookupElementForTypedDefinition(definition : ScTypedDefinition, logger : Logger) : LookupElement =
  val name = definition.name
  val typeName = definitionTypeString(definition, logger)

  LookupElementBuilder
    .create(name)
    .withTypeText(typeName)
    .withIcon(AllIcons.Nodes.Property)
end makeLookupElementForTypedDefinition

def definitionTypeString(definition : ScTypedDefinition, logger : Logger) : String =
  definition
    .`type`()
    .map(unwrapMillTask)
    .map(_.toString())
    .leftMap(
      error =>
        logEncounterOfAnUnknownTypeWhileMakingLookup(definition, error, logger)
        "Unknown type",
    )
    .merge
end definitionTypeString

def logEncounterOfAnUnknownTypeWhileMakingLookup(definition : ScTypedDefinition, failure : Failure, logger : Logger) : Unit =
  enclosingEntity(definition) match
    case Some(value) =>
      logger.debug(
        s"Got an unknown type in attempt to build lookup element for ${definition.name} from ${value.getPresentationName}:\n ${failure.toString()}"
      )
    case None =>
      logger.debug(
        s"Got an unknown type in attempt to build lookup element for ${definition.name}:\n ${failure.toString()}"
      )
end logEncounterOfAnUnknownTypeWhileMakingLookup

/**
 * Tries to look up place where member was defined
 */
def enclosingEntity : ScTypedDefinition => Option[ScTemplateDefinition] =
  case inNameContext(member: ScMember) =>
    Option(member.containingClass)
  case _ =>
    None
end enclosingEntity
