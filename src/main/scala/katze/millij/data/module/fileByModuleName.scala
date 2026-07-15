package katze.millij.data.module

import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.indexing.FileBasedIndex
import katze.millij.data.{ScalaIdentifier, SegmentedPath, Smart}
import org.jetbrains.yaml.psi.YAMLFile

import scala.collection.mutable.ListBuffer

private def fileByModuleName(
  scope : GlobalSearchScope,
  path : SegmentedPath[List, ScalaIdentifier]
)(
  using Smart
) : List[(ModuleDeclaration[ScalaIdentifier], YAMLFile)] =
  val index = FileBasedIndex.getInstance()
  val results = ListBuffer.empty[(ModuleDeclaration[ScalaIdentifier], YAMLFile)]
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
