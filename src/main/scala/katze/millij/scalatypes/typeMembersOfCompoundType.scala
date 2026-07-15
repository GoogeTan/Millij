package katze.millij.scalatypes

import katze.millij.data.{ResolvedSymbol, ScalaIdentifier, Smart}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScClass, ScObject, ScTrait}
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef.TypeDefinitionMembers
import org.jetbrains.plugins.scala.lang.psi.types.api.designator.{ScDesignatorType, ScProjectionType}
import org.jetbrains.plugins.scala.lang.psi.types.{ScCompoundType, ScType}
import org.jetbrains.plugins.scala.project.ProjectContext

/**
 * Gets all class, object and trait definitions in the type
 */
def typeMembersOfCompoundType(
  scType : ScCompoundType
)(using Smart) : List[(name : ScalaIdentifier, psiElement : ScTrait | ScClass | ScObject)] =
  TypeDefinitionMembers.getTypes(scType, None)
    .allSignatures
    .map(_.namedElement)
    .collect[(ScalaIdentifier, ScTrait | ScClass | ScObject)]:
      case thing @ (_ : ScTrait | _ : ScClass | _ : ScObject) =>
        (ScalaIdentifier.unsafe(thing.getName()), thing)
    .toList
end typeMembersOfCompoundType

/**
 * Gets all class, object and trait definitions in the type
 */
def typeMembersOf(
  scType : ScType
)(using ProjectContext, Smart) : List[(name : ScalaIdentifier, psiElement : ScTrait | ScClass | ScObject)] =
  typeMembersOfCompoundType(scType.asCompoundType)
end typeMembersOf

/**
 * Searches for type-like(traits, classes and objects) members in a type.
 */
def resolveTypeMember(scType : ScType, name: ScalaIdentifier)(using ProjectContext, Smart) : Option[ResolvedSymbol[ScalaIdentifier]] =
  typeMembersOf(scType)
    .find(_.name == name)
    .collect {
      case (_, tr: ScTrait) =>
        ResolvedSymbol.ScalaTrait(Some(scType), tr)
      case (_, ob: ScObject) =>
        ResolvedSymbol.ScalaObject(Some(scType), ob)
      case (_, cl: ScClass) =>
        ResolvedSymbol.ScalaClass(Some(scType), cl)
    }
end resolveTypeMember