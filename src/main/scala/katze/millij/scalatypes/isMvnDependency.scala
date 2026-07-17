package katze.millij.scalatypes

import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.plugins.scala.lang.psi.types.{ScType, ScTypeExt}

def isMvnDependency(tie : ScType) : Boolean =
  tie
    .extractClass
    .exists(InheritanceUtil.isInheritor(_, "mill.javalib.Dep"))
end isMvnDependency
