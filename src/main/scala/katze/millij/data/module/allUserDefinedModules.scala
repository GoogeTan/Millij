package katze.millij.data.module

import cats.Foldable
import cats.data.NonEmptyList
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import katze.millij.data.*
import katze.millij.data.module.ModuleDeclaration
import katze.millij.util.findYamlKeyValue
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScClass, ScObject, ScTrait, ScTypeDefinition}
import org.jetbrains.plugins.scala.lang.psi.impl.toplevel.typedef.TypeDefinitionMembers
import org.jetbrains.plugins.scala.lang.psi.types.api.designator.{ScDesignatorType, ScProjectionType}
import org.jetbrains.plugins.scala.lang.psi.types.{ScCompoundType, ScType, TypeSignature}
import org.jetbrains.plugins.scala.project.ProjectContext
import org.jetbrains.yaml.psi.{YAMLFile, YAMLMapping}

import scala.collection.mutable.ListBuffer
import scala.jdk.CollectionConverters.*

/**
 * Returns all the modules defined by the user in the whole project.
 */
def allUserDefinedModules(scope : GlobalSearchScope)(using Smart) : List[ModuleDeclaration[String]] =
  val index = FileBasedIndex.getInstance()
  val results = ListBuffer.empty[ModuleDeclaration[String]]//TODO replace with thread safe ConcurrentLinkedQueue

  index.processAllKeys(YamlModuleIndex.Name, { path =>
    index.processValues(
      YamlModuleIndex.Name,
      path,
      null,
      { (file, value) =>
        results += value
        true
      },
      scope)
    true
  }, scope, null)

  results.toList
end allUserDefinedModules
