package katze.millij.data

import cats.data.NonEmptyList
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.util.{CachedValueProvider, CachedValuesManager, PsiModificationTracker}
import katze.millij.scalatypes.{millApiModuleName, millConfigModule}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScClass, ScTrait}

import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.*

@Service(Array(Service.Level.PROJECT))
final class OverridableTraitsService(project: Project):
  private val cache = CachedValuesManager.getManager(project).createCachedValue[Option[List[ScalaSegmentedPath[NonEmptyList]]]] {
    () =>
      Smart(project) {
        val res = millConfigModule(project).flatMap(millModule =>
          val buildScope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(millModule)
          project.getService(classOf[TypeSearchCache])
            .searchPsiClassDumb(millApiModuleName)
            .map(baseModule =>
              ClassInheritorsSearch.search(baseModule, buildScope, true)
                .findAll().asScala.toList
                .collect:
                  case s: ScTrait => s
                  case c: ScClass => c
                .map(_.getQualifiedName)
                .filter(_ != null)
                .flatMap(ScalaSegmentedPath.fromQualifiedNonEmpty)
            )
        )
        CachedValueProvider.Result.create(
          res,
          ProjectRootManager.getInstance(project)
        )
      }.getOrElse(
        //This branch will never happen as the only method which can access this cache is annotated as smart.  
        CachedValueProvider.Result.create(
          None
        )
      )
  }
  end cache

  def searchForOverridableTraits(using Smart): Option[List[ScTrait | ScClass]] =
    val service = project.getService(classOf[TypeSearchCache])
    cache.getValue.map(
      _
        .flatMap(service.searchPsiClass)
        .collect:
          case s: ScTrait => s
          case c: ScClass => c
    )
  end searchForOverridableTraits
end OverridableTraitsService

