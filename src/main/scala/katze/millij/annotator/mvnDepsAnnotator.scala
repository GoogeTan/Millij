package katze.millij.annotator

import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.util.InheritanceUtil
import katze.millij.{PlaceInYamlConfig, richScopeOf}
import org.jetbrains.plugins.scala.lang.psi.types.{ScType, ScTypeExt}
import org.jetbrains.yaml.psi.YAMLScalar

def isMvnDependency(tie : ScType) : Boolean =
  tie
    .extractClass
    .map(InheritanceUtil.isInheritor(_, "mill.javalib.Dep"))
    .getOrElse(false)
end isMvnDependency

def mvnDepsAnnotator : CoolAnnotator[YAMLScalar, EmptyTuple] =
  case (scalar, _, annotationHolder) =>
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
