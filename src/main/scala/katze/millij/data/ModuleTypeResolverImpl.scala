package katze.millij.data

import cats.data.NonEmptyList
import cats.syntax.all.*
import cats.{Eq, Show}
import com.intellij.psi.PsiPackage
import katze.millij.data.module.{ModuleDeclaration, NamespacedPath}
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.project.ProjectContext
import org.jetbrains.yaml.psi.YAMLMapping

import scala.collection.mutable

final class ModuleTypeResolverImpl[Segment : {Eq, Show}](
  resolveScalaDependency : Smart ?=> (SegmentedPath[List, Segment], Segment) => Option[ResolvedSymbol[Segment]],
  resolveInTypeDependency : Smart ?=> (ScType, Segment) => Option[ResolvedSymbol[Segment]],
  resolvePackageMember : Smart ?=> (PsiPackage, Segment) => Option[ResolvedSymbol[Segment]],
  findYamlModuleMapping : Smart ?=> SegmentedPath[List, Segment] => Option[YAMLMapping],
  buildPrefix : Segment,
  cacheResolvedModule : (SegmentedPath[List, Segment], => ModuleType[Segment]) => ModuleType[Segment],
  rawModuleByName : SegmentedPath[List, Segment] => Option[ModuleDeclaration[Segment]]
)(using ProjectContext) extends ModuleTypeResolver[Segment]:

  override def typeModule(module: ModuleDeclaration[Segment])(using Smart): ModuleType[Segment] =
    typeModule(module, Nil)
  end typeModule

  def typeModule(module: ModuleDeclaration[Segment], resolution : List[SegmentedPath[List, Segment]])(using Smart) : ModuleType[Segment] =
    if resolution.contains(module.segmentedPath) then
      //TODO do something in recursive cases.
      throw new NotImplementedError(s"Recursive dependencies handling is not implemented yet. Cycle length: ${resolution.size}. The cycle: ${resolution.map(_.asQualified).mkString(" -> ")}")
    end if
    cacheResolvedModule(
      module.segmentedPath,
      {
        // Root module can not depend on other modules. So the search is made only in scala space.
        val resolvedSupers =
          if module.segmentedPath.length == 0 then
            module.superTypes.map(parent =>
              resolveScalaQualifiedPath(module.path, parent, module.segmentedPath :: resolution)
            )
          else
            module.superTypes.map(parent =>
              resolvePath(module.path, parent, module.segmentedPath :: resolution)
            )
        ModuleType(resolvedSupers)
      }
    )
  end typeModule


  def resolveScalaQualifiedPath(
    ownPath : NamespacedPath[List, Segment],
    path : SegmentedPath[NonEmptyList, Segment],
    resolution : List[SegmentedPath[List, Segment]]
  )(using Smart) : ResolvedPath[List, Segment, ResolvedSymbol[Segment]] =
    path.resolve(
      resolveScalaDependency(SegmentedPath(Nil), _),
      findMember(_, _, resolution)
    )
  end resolveScalaQualifiedPath

  override def resolvePath(module: NamespacedPath[List, Segment], path: SegmentedPath[NonEmptyList, Segment])(using Smart): ResolvedPath[List, Segment, ResolvedSymbol[Segment]] =
    resolvePath(module, path, Nil)
  end resolvePath

  def resolvePath(
    ownPath : NamespacedPath[List, Segment],
    path : SegmentedPath[NonEmptyList, Segment],
    resolution : List[SegmentedPath[List, Segment]]
  )(using Smart) : ResolvedPath[List, Segment, ResolvedSymbol[Segment]] =
    path.resolve(
      resolveOne(ownPath, _, resolution),
      findMember(_, _, resolution)
    )
  end resolvePath

  def resolveOne(
    ownPath: NamespacedPath[List, Segment],
    whatToSearchFor: Segment,
    resolution : List[SegmentedPath[List, Segment]]
  )(using Smart): Option[ResolvedSymbol[Segment]] =
    ownPath.searchScopes
      .collectFirstSome(scope =>
        rawModuleByName(scope).flatMap(moduleMetadata =>
          val moduleType = typeModule(moduleMetadata, resolution)
          findModuleMember(scope, moduleType, whatToSearchFor, resolution)
        )
      )
      .orElse(resolveScalaDependency(SegmentedPath(Nil), whatToSearchFor))
  end resolveOne

  def findModuleMember(
    modulePath : SegmentedPath[List, Segment],
    resolvedThing: ModuleType[Segment],
    memberName : Segment,
    resolution : List[SegmentedPath[List, Segment]]
  )(using Smart) : Option[ResolvedSymbol[Segment]] =
    // "build" is the name of the root module.
    // Every each for inner module also goes to the root module so the easiest way
    // to work with fully qualified paths is to add this code.
    if modulePath.length == 0 && memberName == buildPrefix then
      resolvedSybmolForModule(modulePath, resolution)
    else
      val fullPath = modulePath.add(memberName)
      resolvedSybmolForModule(fullPath, resolution)
        .orElse(
          resolveInTypeDependency(
            resolvedThing.ownType,
            memberName
          )
        )
  end findModuleMember

  def resolvedSybmolForModule(name : SegmentedPath[List, Segment], resolution : List[SegmentedPath[List, Segment]])(using Smart) : Option[ResolvedSymbol[Segment]] =
    rawModuleByName(name)
      .map(typeModule(_, resolution))
      .flatMap(moduleType =>
        findYamlModuleMapping(name)
          .map(mapping =>
            ResolvedSymbol.YamlModule(name, moduleType, mapping)
          )
      )
  end resolvedSybmolForModule

  def findMember(
    resolvedSymbol: ResolvedSymbol[Segment],
    memberName : Segment,
    resolution : List[SegmentedPath[List, Segment]]
  )(using Smart, ProjectContext) : Option[ResolvedSymbol[Segment]] =
    resolvedSymbol.scTypeForQualifiedSearch match
      case Some(scType) =>
        resolveInTypeDependency(scType, memberName)
      case None =>
        resolvedSymbol match
          case ResolvedSymbol.ScalaPackage(element) =>
            resolvePackageMember(element, memberName)
          case ResolvedSymbol.YamlModule(modulePath, moduleType, _) =>
            findModuleMember(modulePath, moduleType, memberName, resolution)
        end match
    end match
  end findMember
end ModuleTypeResolverImpl

