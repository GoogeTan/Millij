package katze.millij.scalatypes

import cats.data.NonEmptyList
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import katze.millij.data.*
import katze.millij.data.module.*
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScClass, ScTrait}
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.project.ProjectContext

import scala.jdk.CollectionConverters.*

val millApiModuleName : SegmentedPath[NonEmptyList, ScalaIdentifier] =
  SegmentedPath.fromQualifiedNonEmpty("mill.api.Module").get.map(ScalaIdentifier.unsafe)

/**
 * Finds all subtypes of mill.api.Module.
 * @param project
 * @return
 */
def searchForOverridableTraits(project: Project)(using Smart): Option[List[ScTrait | ScClass]] =
  project.getService(classOf[OverridableTraitsService])
    .searchForOverridableTraits
end searchForOverridableTraits

//TODO cache this too
def searchForDependentOverridableTraits(
  context : NamespacedPath[List, ScalaIdentifier],
  project: Project
)(using Smart): List[(name : SegmentedPath[NonEmptyList, ScalaIdentifier], scType : ScType, psiElement : ScTrait | ScClass)] =
  given ProjectContext = ProjectContext.fromProject(project)
  val millModuleService = project.getService(classOf[MillModuleService])
  for
    moduleMetadata <- allUserDefinedModules(GlobalSearchScope.allScope(project))
    typedModule = millModuleService.typeModule(moduleMetadata)
    res <- nestedTraitMembersOf(
      typedModule.ownType,
      moduleMetadata.segmentedPath
    )
  yield res
end searchForDependentOverridableTraits