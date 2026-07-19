package katze.millij.annotator

import com.intellij.lang.annotation.{AnnotationHolder, HighlightSeverity}
import katze.millij.data.module.NamespacedPath
import katze.millij.data.{MillModuleService, Smart}
import katze.millij.psi.{YAMLKeyValueWithKey, YAMLMillModule}
import katze.millij.file.*
import katze.millij.place.*
import org.jetbrains.yaml.psi.impl.YAMLValueImpl
import org.jetbrains.yaml.psi.{YAMLAlias, YAMLCompoundValue, YAMLMapping, YAMLScalar, YAMLSequence, YAMLSequenceItem}

import scala.jdk.CollectionConverters.*

def extendsBlockMembersAnnotator(
  annotateScalar : (
    enclosingMod : NamespacedPath[List, String],
    scalar : YAMLScalar,
    annotationHolder : AnnotationHolder, 
    isOnCycle : Boolean
  ) => Unit
)(using Smart):
  CoolAnnotator[
    (
      YAMLKeyValueWithKey["extends"],
      YAMLMapping,
      YAMLMillModule
    )
  ] =
  case ((element, _, module), annotationHolder) =>
    for
      enclosingMod <- enclosingModule(element)
      service = element.getProject.getService(classOf[MillModuleService])
      enclosingModuleType <- service.resolveModuleByName(enclosingMod)
      cycles = enclosingModuleType.cyclesOfDependnecies
      problematicDependencies = cycles.map(_.whatModuleDependsOn(enclosingMod.fullPath).get/*We can use get here as it is guaranteed that cycle contains the module*/)
      scalar <- element.getValue match
        case scalar : YAMLScalar => List(scalar)
        case lst : YAMLSequence =>
          lst
            .getItems.asScala
            .map(_.getValue)
            .collect:
              case s : YAMLScalar => s
        case _ =>
          Nil
    do 
      val isProblematic =
        problematicDependencies.exists(
          problematicSuper =>
            scalar.getTextValue.trim.contains(problematicSuper.asQualified)
        )
      annotateScalar(
        enclosingMod,
        scalar,
        annotationHolder,
        isProblematic
      )
    end for
end extendsBlockMembersAnnotator