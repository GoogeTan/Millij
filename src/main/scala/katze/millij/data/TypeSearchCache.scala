package katze.millij.data

import cats.Foldable
import cats.data.NonEmptyList
import cats.syntax.all.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.{CachedValueProvider, CachedValuesManager, PsiModificationTracker}
import com.intellij.psi.{JavaPsiFacade, PsiClass, PsiPackage}
import katze.millij.scalatypes.makePossibleImports
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTypeDefinition
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.lang.psi.types.api.designator.ScDesignatorType
import org.jetbrains.plugins.scala.project.ProjectContext

import java.util.concurrent.ConcurrentHashMap
import scala.annotation.targetName

@Service(Array(Service.Level.PROJECT))
final class TypeSearchCache(project: Project):
  private val cache = CachedValuesManager.getManager(project).createCachedValue { () =>
    CachedValueProvider.Result.create(
      new ConcurrentHashMap[String, Option[PsiClass]](),
      PsiModificationTracker.MODIFICATION_COUNT,
      com.intellij.openapi.roots.ProjectRootManager.getInstance(project)
    )
  }

  def searchPsiClassDumb(text: SegmentedPath[NonEmptyList, String])(using Smart): Option[PsiClass] =
    val map = cache.getValue
    map.computeIfAbsent(
      text.asQualified,
      ScalaPsiManager.instance(project).getCachedClass(GlobalSearchScope.allScope(project), _)
    )
  end searchPsiClassDumb
  
  def searchPsiClass(path : SegmentedPath[NonEmptyList, String])(using Smart) : Option[PsiClass] =
    makePossibleImports(path).collectFirstSome(searchPsiClassDumb)
  end searchPsiClass
  
  def findPackageDumb(text : SegmentedPath[NonEmptyList, String])(using Smart) : Option[PsiPackage] =
    val facade = JavaPsiFacade.getInstance(project)
    Option(facade.findPackage(text.asQualified))
  end findPackageDumb
  
  def findPackages(text : SegmentedPath[NonEmptyList, String])(using Smart) : List[PsiPackage] =
    makePossibleImports(text).flatMap(findPackageDumb)//TODO add scala package objects and other thing support
  end findPackages
  
  def searchSkType(path : SegmentedPath[NonEmptyList, String])(using Smart): Option[ScType] =
    searchPsiClass(path).map(ScDesignatorType(_))
  end searchSkType
  
  def searchSkType(path : String)(using Smart): Option[ScType] = 
    SegmentedPath.fromQualifiedNonEmpty(path)
      .flatMap(searchSkType)
  end searchSkType
end TypeSearchCache