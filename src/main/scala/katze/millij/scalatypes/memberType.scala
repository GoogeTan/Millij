package katze.millij.scalatypes

import org.jetbrains.plugins.scala.lang.psi.api.statements.{ScFunction, ScValue, ScVariable}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScClass
import org.jetbrains.plugins.scala.lang.psi.types.ScType

/**
 * Searches the base ScType for a method with 0 arguments, a field matching the name,
 * or a primary constructor parameter and returns it's type.
 */
def findMemberType(baseType: ScType, fieldName: String): Option[ScType] =
  extractTemplateDefinition(baseType).flatMap { template =>
    val directMatch = template.members.iterator.collectFirst {
      case fn: ScFunction if fn.parameters.isEmpty && fn.name == fieldName =>
        fn.returnType.toOption

      case v: ScValue if v.declaredElements.exists(_.name == fieldName) =>
        v.declaredElements.find(_.name == fieldName).flatMap(_.`type`().toOption)

      case v: ScVariable if v.declaredElements.exists(_.name == fieldName) =>
        v.declaredElements.find(_.name == fieldName).flatMap(_.`type`().toOption)
    }.flatten

    val constructorMatch = template match
      case clazz: ScClass =>
        clazz.parameters.find(_.name == fieldName).flatMap(_.`type`().toOption)
      case _ =>
        None

    lazy val inheritedMatch = template.allMethods.iterator.map(_.method).collectFirst {
      case fn: ScFunction if fn.parameters.isEmpty && fn.name == fieldName =>
        fn.returnType.toOption
    }.flatten

    directMatch.orElse(constructorMatch).orElse(inheritedMatch)
  }
end findMemberType

