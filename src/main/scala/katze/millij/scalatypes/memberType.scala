package katze.millij.scalatypes

import katze.millij.data.Smart
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef.TypeDefinitionMembers
import org.jetbrains.plugins.scala.lang.psi.types.{ScType, TermSignature}
import org.jetbrains.plugins.scala.project.ProjectContext

/**
 * Searches the base ScType for a signature matching the fieldName and returns its resolved return type.
 */
def findMemberType(baseType: ScType, fieldName: String)(using Smart): Option[ScType] =
  given ProjectContext = baseType.projectContext
  TypeDefinitionMembers.getSignatures(baseType.asCompoundType, None)
    .allSignatures
    .find(_.name == fieldName)
    .flatMap(termSignatureType)
end findMemberType
