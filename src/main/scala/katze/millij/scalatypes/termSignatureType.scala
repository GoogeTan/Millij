package katze.millij.scalatypes

import katze.millij.data.Smart
import org.jetbrains.plugins.scala.lang.psi.api.statements.ScFunction
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScTypedDefinition
import org.jetbrains.plugins.scala.lang.psi.types.{ScType, TermSignature}
import org.jetbrains.plugins.scala.project.ProjectContext

/**
 * Calculates the return type of a TermSignature, applying its substitutor.
 */
def termSignatureType(term: TermSignature)(using ProjectContext, Smart): Option[ScType] =
  term.intersectedReturnType.orElse {
    val rawTypeOpt = term.namedElement match
      case fn: ScFunction =>
        fn.returnType.toOption
      case typed: ScTypedDefinition =>
        typed.`type`().toOption
      case _ =>
        None

    rawTypeOpt.map(term.substitutor)
  }
end termSignatureType
