package katze.millij.scalatypes

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager
import scala.jdk.CollectionConverters.*

/**
 * Finds all subtypes of mill.api.Module.
 * @param project
 * @return
 */
def searchForOverridableTraits(project: Project): Option[List[PsiClass]] =
  millConfigModule(project).flatMap(millModule =>
    val buildScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(millModule)
    val facade = ScalaPsiManager.instance(project)

    facade.getCachedClass(buildScope, "mill.api.Module").map(baseModule =>
      ClassInheritorsSearch.search(baseModule, buildScope, true).findAll().asScala.toList
    )
  )
end searchForOverridableTraits
