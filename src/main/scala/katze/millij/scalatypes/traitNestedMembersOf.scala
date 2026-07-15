package katze.millij.scalatypes

import cats.data.NonEmptyList
import katze.millij.data.{ScalaIdentifier, SegmentedPath, Smart}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScClass, ScObject, ScTrait}
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef.TypeDefinitionMembers
import org.jetbrains.plugins.scala.lang.psi.types.api.designator.ScProjectionType
import org.jetbrains.plugins.scala.lang.psi.types.{ScCompoundType, ScType}
import org.jetbrains.plugins.scala.project.ProjectContext

/**
 * Gets all class, object and trait definitions in the type
 */
def nestedTraitMembersOf(
  scType : ScType,
  initialPath : SegmentedPath[List, ScalaIdentifier] = SegmentedPath(Nil)
)(using ProjectContext, Smart) : List[(name : SegmentedPath[NonEmptyList, ScalaIdentifier], scType : ScType, psiElement : ScTrait | ScClass)] =
  TypeDefinitionMembers.getTypes(scType.asCompoundType, None)
    .allSignatures
    .map(_.namedElement)
    .flatMap[(name : SegmentedPath[NonEmptyList, ScalaIdentifier], scType : ScType, psiElement : ScTrait | ScClass)]:
      case thing @ (_ : ScTrait | _ : ScClass) =>
        List(
          (
            name = initialPath.addNonEmpty(ScalaIdentifier.unsafe(thing.getName())), 
            scType = ScProjectionType(scType, thing), 
            psiElement = thing
          )
        )
      case scObject : ScObject =>
        nestedTraitMembersOf(
          ScProjectionType(scType, scObject),
          initialPath.add(ScalaIdentifier.unsafe(scObject.getName()))
        )
      case _ => Nil  
    .toList
end nestedTraitMembersOf
