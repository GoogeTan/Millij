package katze.millij.data

import cats.data.NonEmptyList
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.{CachedValueProvider, CachedValuesManager, PsiModificationTracker}
import com.intellij.util.indexing.FileBasedIndex
import katze.millij.data.module.{YamlModuleIndex, ModuleDeclaration, NamespacedPath, findYamlModuleMapping}
import katze.millij.scalatypes.{resolveTypeMember, resolvePackageMember}
import org.jetbrains.plugins.scala.caches.RecursionManager
import org.jetbrains.plugins.scala.project.ProjectContext

import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.*


@Service(Array(Service.Level.PROJECT))
final class MillModuleService(project: Project):
  val recursionGuard = RecursionManager.RecursionGuard[SegmentedPath[List, ScalaIdentifier], Nothing]("millij.type.resolution")

  private val resolved = CachedValuesManager.getManager(project).createCachedValue { () =>
    CachedValueProvider.Result.create(
      new ConcurrentHashMap[SegmentedPath[List, ScalaIdentifier], ModuleType[ScalaIdentifier]](),
      PsiModificationTracker.MODIFICATION_COUNT,
      com.intellij.openapi.roots.ProjectRootManager.getInstance(project)
    )
  }

  given ProjectContext = ProjectContext.fromProject(project)

  val searchScope = GlobalSearchScope.allScope(project)
  val service = project.getService(classOf[TypeSearchCache])


  val resolver : ModuleTypeResolver[ScalaIdentifier] = ModuleTypeResolverImpl[ScalaIdentifier](
    resolveScalaDependency = (path: SegmentedPath[List, ScalaIdentifier], name: ScalaIdentifier) =>
      service.searchPsiClass(path.addNonEmpty(name))
        .flatMap(ResolvedSymbol.fromPsiElement).orElse(
          service.findPackages(path.addNonEmpty(name))
            .flatMap(ResolvedSymbol.fromPsiElement)
            .headOption //TODO
        ),
    resolveInTypeDependency = resolveTypeMember,
    resolvePackageMember = (psiPackage, element) =>
      resolvePackageMember(psiPackage, element, searchScope).headOption, //TODO do something with multiple options
    buildPrefix = ScalaIdentifier.unsafe("build"),
    findYamlModuleMapping = findYamlModuleMapping(searchScope, _),
    cacheResolvedModule = (key, f) => resolved.getValue.computeIfAbsent(key, _ => f),
    rawModuleByName = name =>
      val found = rawModuleByName(name)
      if found.size == 1 then
        found.headOption
      else
        None
  )

  def rawModuleByName(name : SegmentedPath[List, ScalaIdentifier]) : List[ModuleDeclaration[ScalaIdentifier]] =
    FileBasedIndex.getInstance().getValues(
      YamlModuleIndex.Name,
      name,
      searchScope
    ).asScala.toList
  end rawModuleByName

  def rawModuleByName(name : NamespacedPath[List, ScalaIdentifier]) : Option[ModuleDeclaration[ScalaIdentifier]] =
    val map = FileBasedIndex.getInstance().getFileData(
      YamlModuleIndex.Name,
      ???,//TODO
      project
    )
    val fullPath = name.namespace
    if map.containsKey(fullPath) then
      Some(map.get(fullPath))
    else
      None
  end rawModuleByName

  def resolveModuleByName(name : SegmentedPath[List, ScalaIdentifier])(using Smart) : List[ModuleType[ScalaIdentifier]] =
    rawModuleByName(name).map(typeModule)
  end resolveModuleByName

  def typeModule(module: ModuleDeclaration[ScalaIdentifier])(using Smart): ModuleType[ScalaIdentifier] =
    resolver.typeModule(module)
  end typeModule

  def resolvePath(
    module : NamespacedPath[List, ScalaIdentifier],
    path : SegmentedPath[NonEmptyList, ScalaIdentifier]
  )(using Smart) : ResolvedPath[List, ScalaIdentifier, ResolvedSymbol[ScalaIdentifier]] =
    resolver.resolvePath(module, path)
  end resolvePath
end MillModuleService
