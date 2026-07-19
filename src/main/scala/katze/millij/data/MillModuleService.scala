package katze.millij.data

import cats.data.NonEmptyList
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.{CachedValueProvider, CachedValuesManager, PsiModificationTracker}
import com.intellij.util.indexing.FileBasedIndex
import katze.millij.data.module.{ModuleDeclaration, NamespacedPath, YamlModuleIndex, findYamlModuleMapping}
import katze.millij.scalatypes.{resolvePackageMember, resolveTypeMember}
import org.jetbrains.plugins.scala.caches.RecursionManager
import org.jetbrains.plugins.scala.project.ProjectContext
import katze.millij.file.*

import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.*


@Service(Array(Service.Level.PROJECT))
final class MillModuleService(project: Project):
  val recursionGuard = RecursionManager.RecursionGuard[SegmentedPath[List, String], Nothing]("millij.type.resolution")

  private val resolved = CachedValuesManager.getManager(project).createCachedValue { () =>
    CachedValueProvider.Result.create(
      new ConcurrentHashMap[SegmentedPath[List, String], ModuleType[String]](),
      PsiModificationTracker.MODIFICATION_COUNT,//TODO use something more specific. Like own modification counter or resolve types into descriptions(e.g. Scala sclass names, file paths and etc).
      com.intellij.openapi.roots.ProjectRootManager.getInstance(project)
    )
  }

  given ProjectContext = ProjectContext.fromProject(project)

  val searchScope = GlobalSearchScope.allScope(project)
  val service = project.getService(classOf[TypeSearchCache])


  val resolver : ModuleTypeResolver[String] = ModuleTypeResolverImpl[String](
    resolveScalaDependency = (path: SegmentedPath[List, String], name: String) =>
      service.searchPsiClass(path.addNonEmpty(name))
        .flatMap(ResolvedSymbol.fromPsiElement).orElse(
          service.findPackages(path.addNonEmpty(name))
            .flatMap(ResolvedSymbol.fromPsiElement)
            .headOption //TODO
        ),
    resolveInTypeDependency = resolveTypeMember,
    resolvePackageMember = (psiPackage, element) =>
      resolvePackageMember(psiPackage, element, searchScope).headOption, //TODO do something with multiple options
    buildPrefix = "build",
    findYamlModuleMapping = findYamlModuleMapping(searchScope, _),
    cacheResolvedModule = (key, f) => resolved.getValue.computeIfAbsent(key, _ => f),
    rawModuleByName = name =>
      val found = rawModuleByName(name)
      if found.size == 1 then
        found.headOption
      else
        None
  )

  def rawModuleByName(name : SegmentedPath[List, String]) : List[ModuleDeclaration[String]] =
    FileBasedIndex.getInstance().getValues(
      YamlModuleIndex.Name,
      name,
      searchScope
    ).asScala.toList
  end rawModuleByName

  def rawModuleByName(name : NamespacedPath[List, String]) : Option[ModuleDeclaration[String]] =
    val moduleFileName =
      name.namespace.addNonEmpty("build.mill.yaml")
    virtualFileByRelativePath(
      project,
      moduleFileName
    ).flatMap:
      file =>
        val found = FileBasedIndex.getInstance().getFileData(
          YamlModuleIndex.Name,
          file,
          project
        )
        Option(found.get(name.fullPath))
  end rawModuleByName

  def resolveModuleByName(name : SegmentedPath[List, String])(using Smart) : List[ModuleType[String]] =
    rawModuleByName(name).map(typeModule)
  end resolveModuleByName

  def resolveModuleByName(name : NamespacedPath[List, String])(using Smart) : Option[ModuleType[String]] =
    rawModuleByName(name).map(typeModule)
  end resolveModuleByName

  def typeModule(module: ModuleDeclaration[String])(using Smart): ModuleType[String] =
    resolver.typeModule(module)
  end typeModule

  def resolvePath(
    module : NamespacedPath[List, String],
    path : SegmentedPath[NonEmptyList, String]
  )(using Smart) : ResolvedPath[List, String, ResolvedSymbol[String]] =
    resolver.resolvePath(module, path)
  end resolvePath
end MillModuleService
