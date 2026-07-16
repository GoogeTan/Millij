package katze.millij.scalatypes

import katze.millij.data.Smart
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScFunction, ScValue, ScVariable}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScTypedDefinition
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScClass, ScTemplateDefinition}
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef.TypeDefinitionMembers
import org.jetbrains.plugins.scala.lang.psi.types.{ScType, TermSignature}
import org.jetbrains.plugins.scala.project.ProjectContext

/**
 * Returns all the members which have zero parameters: methods without parameters, vars, vals and primary contractor arguments.
 * Basically those are all things that can be defined in YAML config.
 */
def yamlDefinableMembersOf(scType: ScType)(using Smart, ProjectContext): Seq[TermSignature] =
  val ignoredBaseClasses = Set(
    "java.lang.Object",
    "scala.Any",
    "scala.AnyRef",
    "scala.AnyVal",
    "scala.Product",
    "scala.Equals"
  )
  (
    for
      signature <- TypeDefinitionMembers.getSignatures(scType.asCompoundType, None).allSignatures
      if !ignoredBaseClasses.contains(signature.containingClass.getQualifiedName)
      if signature.paramLength == 0
    yield signature
  ).toList
end yamlDefinableMembersOf
