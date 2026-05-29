package katze.millij.data

import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.{CachedValueProvider, CachedValuesManager, PsiModificationTracker}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.lang.psi.types.api.designator.ScDesignatorType

import java.util.concurrent.ConcurrentHashMap

@Service(Array(Service.Level.PROJECT))
final class TypeSearchCache(project: Project):
  private val cache = CachedValuesManager.getManager(project).createCachedValue { () =>
    CachedValueProvider.Result.create(
      new ConcurrentHashMap[String, Option[PsiClass]](),
      PsiModificationTracker.MODIFICATION_COUNT
    )
  }

  def searchPsiClass(text : String) : Option[PsiClass] =
    val map = cache.getValue
    map.computeIfAbsent(
      text,
      ScalaPsiManager.instance(project).getCachedClass(GlobalSearchScope.allScope(project), _)
    )
  end searchPsiClass

  def searchSkType(text: String): Option[ScType] =
    searchPsiClass(text).map(ScDesignatorType(_))
  end searchSkType
end TypeSearchCache
