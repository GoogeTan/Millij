package katze.millij.completions.providers

import cats.syntax.all.*
import com.intellij.codeInsight.completion.{CompletionParameters, CompletionResultSet}
import com.intellij.codeInsight.lookup.{LookupElement, LookupElementBuilder}
import com.intellij.icons.AllIcons
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.ProcessingContext
import katze.millij.completions.cool.CoolCompletionProvider
import katze.millij.data.Smart
import katze.millij.place.{richPlaceOf, yamlDefinableMembersOfScope}
import katze.millij.psi.CompletionPosition
import katze.millij.scalatypes.{termSignatureType, unwrapMillTask}
import org.jetbrains.plugins.scala.lang.psi.ScalaPsiUtil.inNameContext
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScTypedDefinition
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScMember, ScTemplateDefinition}
import org.jetbrains.plugins.scala.lang.psi.types.result.Failure
import org.jetbrains.yaml.psi.YAMLPsiElement
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScTypeAliasDefinition
import org.jetbrains.plugins.scala.lang.psi.types.{AliasType, ScType, ScTypeExt, TermSignature, TypePresentationContext}
import org.jetbrains.plugins.scala.project.ProjectContext

/**
 * Adds completions for module members and object params.
 */
def memberCompletionProvider(logger : Logger)(using Smart) : CoolCompletionProvider[CompletionPosition, YAMLPsiElement] =
  case (
    parameters: CompletionParameters,
    psiElement: CompletionPosition,
    yamlElement,
    context: ProcessingContext,
    resultSet: CompletionResultSet
  ) =>
    given ProjectContext = ProjectContext.fromPsi(psiElement)
    richPlaceOf(yamlElement) match
      case Left(errValue) =>
        logger.debug(s"Couldn't build completions for $psiElement of class ${psiElement.getClass.getSimpleName}:\n ${errValue}")
      case Right(scope) =>
        yamlDefinableMembersOfScope(scope)
          .filter(member => !scope.definedMembers.contains(member.name))
          .map(makeLookupElementForTypedDefinition(_, logger))
          .foreach(resultSet.addElement)
    end match
end memberCompletionProvider

def makeLookupElementForTypedDefinition(term : TermSignature, logger : Logger)(using Smart, ProjectContext) : LookupElement =
  given TypePresentationContext = TypePresentationContext(term.namedElement)
  val typeName = termSignatureType(term).map(definitionTypeString(_, logger))

  typeName.fold(
    LookupElementBuilder
      .create(term.name)
      .withIcon(AllIcons.Nodes.Property)
  )(typeName =>
    LookupElementBuilder
      .create(term.name)
      .withTypeText(typeName)
      .withIcon(AllIcons.Nodes.Property)
  )
end makeLookupElementForTypedDefinition

def definitionTypeString(scType : ScType, logger : Logger)(using Smart, TypePresentationContext) : String =
  fullyDealias(unwrapMillTask(scType)).presentableText
end definitionTypeString

def fullyDealias(tpe: ScType): ScType =
  tpe match
    case AliasType(alias: ScTypeAliasDefinition, _, Right(upper: ScType), _) => fullyDealias(upper)
    case _ => tpe
end fullyDealias