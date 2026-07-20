package katze.millij.scalatypes

import cats.data.NonEmptyList
import katze.millij.data.Smart
import katze.millij.path.SegmentedPath
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
  initialPath : SegmentedPath[List, String] = SegmentedPath(Nil)
)(using ProjectContext, Smart) : List[(name : SegmentedPath[NonEmptyList, String], scType : ScType, psiElement : ScTrait | ScClass)] =
  TypeDefinitionMembers.getTypes(scType.asCompoundType, None)
    .allSignatures
    .map(_.namedElement)
    .flatMap[(name : SegmentedPath[NonEmptyList, String], scType : ScType, psiElement : ScTrait | ScClass)]:
      case thing @ (_ : ScTrait | _ : ScClass) =>
        List(
          (
            name = initialPath.addNonEmpty(thing.getName()), 
            scType = ScProjectionType(scType, thing), 
            psiElement = thing
          )
        )
      case scObject : ScObject =>
        nestedTraitMembersOf(
          ScProjectionType(scType, scObject),
          initialPath.add(scObject.getName())
        )
      case _ => Nil  
    .toList
end nestedTraitMembersOf
