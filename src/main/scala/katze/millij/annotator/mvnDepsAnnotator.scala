package katze.millij.annotator

import cats.syntax.all.*
import com.intellij.lang.annotation.{AnnotationHolder, HighlightSeverity}
import com.intellij.psi.util.InheritanceUtil
import com.intellij.structuralsearch.impl.matcher.handlers.AnnotationHandler
import katze.millij.completions.providers.yamlMavenDependenciesPattern
import katze.millij.data.Smart
import katze.millij.place.{PlaceInYamlConfig, richPlaceOf}
import katze.millij.psi.{PsiChild, YAMLExactlyValue}
import org.jetbrains.plugins.scala.lang.psi.types.{ScType, ScTypeExt}
import org.jetbrains.yaml.psi.{YAMLPsiElement, YAMLScalar}

def isMvnDependency(tie : ScType) : Boolean =
  tie
    .extractClass
    .exists(InheritanceUtil.isInheritor(_, "mill.javalib.Dep"))
end isMvnDependency

/**
 * Annotates maven dependency strings(e.g. in mvnDeps blocks).
 */
def mvnDepsAnnotator(using Smart) : CoolAnnotator[(YAMLPsiElement, YAMLPsiElement)] =
  case ((element, parent), annotationHolder) =>
    (richPlaceOf(element), richPlaceOf(parent)) match
      case (
        _,
        Right(PlaceInYamlConfig.Member(_, parentExpectedType, _))
      ) if isMvnDependency(parentExpectedType) =>
        ()
        /*
         if parent is expected to have maven dependency value too then we are in case when the dependency was parsed as key valie pair:
        ```yaml
        mvnDeps:
          - a.b:<caret>
        ```
        So the whole element was already highlighted earlier.
        */
      case (
        Right(PlaceInYamlConfig.Member(_, expectedType, _)),
        _
      ) if isMvnDependency(expectedType) =>
        annotateMavenDependency(element, annotationHolder)
      case _ =>
        ()
end mvnDepsAnnotator

def annotateMavenDependency(element : YAMLPsiElement, annotationHolder: AnnotationHolder) : Unit =
  annotationHolder
    .newSilentAnnotation(HighlightSeverity.INFORMATION)
    .range(element)
    .textAttributes(MillijTextStyles.MILL_MAVEN_DEPENDENCY)
    .create()
end annotateMavenDependency

