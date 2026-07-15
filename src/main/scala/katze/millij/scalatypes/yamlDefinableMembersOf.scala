package katze.millij.scalatypes

import katze.millij.data.Smart
import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScFunction, ScValue, ScVariable}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.ScTypedDefinition
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScClass, ScTemplateDefinition}

/**
 * Returns all the members which have zero parameters: methods without parameters, vars, vals and primary contractor arguments.
 * Basically those are all things that can be defined in YAML config.
 * TODO refactor to use signatures 
 */
def yamlDefinableMembersOf(template: ScTemplateDefinition)(using Smart): Seq[ScTypedDefinition] =
  val ignoredBaseClasses = Set(
    "java.lang.Object",
    "scala.Any",
    "scala.AnyRef",
    "scala.AnyVal",
    "scala.Product",
    "scala.Equals"
  )

  // 1. Get the properties defined in the body { ... }
  val bodyElements: Seq[ScTypedDefinition] =
    template.allMethods.toList
      .map(_.method)
      .filterNot(method =>
        Option(method.getContainingClass).exists { clazz =>
          ignoredBaseClasses.contains(clazz.getQualifiedName)
        }
      )
      .flatMap://TODO support for java methods
        case fn: ScFunction if fn.parameters.isEmpty =>
          Seq(fn)
        case v: ScValue =>
          v.declaredElements
        case v: ScVariable =>
          v.declaredElements
        case _ =>
          Seq.empty

  val constructorParams: Seq[ScTypedDefinition] = template match
    case clazz: ScClass =>
      clazz.parameters
    case _ =>
      Seq.empty

  bodyElements ++ constructorParams
end yamlDefinableMembersOf
