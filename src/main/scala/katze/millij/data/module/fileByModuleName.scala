package katze.millij.data.module

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import katze.millij.data.{SegmentedPath, Smart}
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
