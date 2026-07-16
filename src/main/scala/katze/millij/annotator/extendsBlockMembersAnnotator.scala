package katze.millij.annotator

import com.intellij.lang.annotation.{AnnotationHolder, HighlightSeverity}
import katze.millij.data.module.NamespacedPath
import katze.millij.data.{MillModuleService, ScalaIdentifier, Smart}
import katze.millij.psi.{YAMLKeyValueWithKey, YAMLMillModule}
import katze.millij.file.*
import katze.millij.place.*
import org.jetbrains.yaml.psi.impl.YAMLValueImpl
import org.jetbrains.yaml.psi.{YAMLAlias, YAMLCompoundValue, YAMLMapping, YAMLScalar, YAMLSequence, YAMLSequenceItem}

import scala.jdk.CollectionConverters.*

def extendsBlockMembersAnnotator(
  annotateScalar : (
    enclosingMod : NamespacedPath[List, ScalaIdentifier],
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
      cycle <- enclosingModuleType.cyclesOfDependnecies
      dependsOn <- cycle.whatModuleDependsOn(enclosingMod.fullPath)
      problematicSuper <- enclosingModuleType.dependencies.map(_.segmentedPath).filter(_.startsWith(dependsOn))
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
      annotateScalar(
        enclosingMod,
        scalar,
        annotationHolder,
        scalar.getTextValue.trim.contains(problematicSuper.asQualified)
      )
end extendsBlockMembersAnnotator