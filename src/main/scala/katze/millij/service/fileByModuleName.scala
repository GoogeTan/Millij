package katze.millij.service

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import katze.millij.data.Smart
import katze.millij.module.ModuleDeclaration
import katze.millij.path.SegmentedPath
import katze.millij.service.YamlModuleIndex
import org.jetbrains.yaml.psi.YAMLFile

import scala.collection.mutable.ListBuffer

private def fileByModuleName(
  scope : GlobalSearchScope,
  path : SegmentedPath[List, String]
)(
  using Smart
) : List[(ModuleDeclaration[String], YAMLFile)] =
  val index = FileBasedIndex.getInstance()
  val results = ListBuffer.empty[(ModuleDeclaration[String], YAMLFile)]
  val psiManager = com.intellij.psi.PsiManager.getInstance(scope.getProject)
  index.processValues(
    YamlModuleIndex.Name,
    path,
    null,
    { (virtualFile, value) =>
      Option(psiManager.findFile(virtualFile)).collect {
        case yamlFile: YAMLFile =>
          results.addOne((value, yamlFile))
      }
      true
    },
    scope
  )
  results.toList
end fileByModuleName
