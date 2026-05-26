package katze.millij.annotator

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.util.InheritanceUtil
import katze.millij.place.{PlaceInYamlConfig, richScopeOf}
import org.jetbrains.plugins.scala.lang.psi.types.{ScType, ScTypeExt}
import org.jetbrains.yaml.psi.YAMLScalar

def isMvnDependency(tie : ScType) : Boolean =
  tie
    .extractClass
    .map(InheritanceUtil.isInheritor(_, "mill.javalib.Dep"))
    .getOrElse(false)
end isMvnDependency

/**
 * Annotates maven dependency strings(e.g. in mvnDeps blocks).
 * TODO make it work for partially written strings to
 */
def mvnDepsAnnotator : CoolAnnotator[YAMLScalar] =
  case (scalar, annotationHolder) =>
    // Нас тут не интересуют ошибки вида не нашлось поле extends и так далее. Поэтосу можно забить на ошибки
    richScopeOf(scalar).foreach:
      case PlaceInYamlConfig.Module(_, _) =>
        ()
      case PlaceInYamlConfig.Member(parentTypes, name, expectedType, _) =>
        if isMvnDependency(expectedType) then
            annotationHolder
              .newSilentAnnotation(HighlightSeverity.INFORMATION)
              .range(scalar)
              .textAttributes(MillTextStyles.MAVEN_DEPENDENCY)
              .create()
end mvnDepsAnnotator
