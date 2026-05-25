package katze.millij.scalatypes

import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTemplateDefinition
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager
import org.jetbrains.plugins.scala.lang.psi.types.{ScType, ScTypeExt, extractTypeParameters}
import org.jetbrains.plugins.scala.lang.psi.types.api.designator.ScDesignatorType

def classSearch(psi : ScalaPsiManager, scope : GlobalSearchScope, className : String) : Option[PsiClass] =
  psi.getCachedClass(scope, className)
end classSearch


def classTypeSearch(psi : ScalaPsiManager, scope : GlobalSearchScope, className : String) : Option[ScDesignatorType] =
  classSearch(psi, scope, className).map(ScDesignatorType(_))
end classTypeSearch

def extractTemplateDefinition(psiClass: PsiClass): Option[ScTemplateDefinition] =
  psiClass match
    case templateDef: ScTemplateDefinition => Some(templateDef)
    case _ => None
end extractTemplateDefinition

def extractTemplateDefinition(psiClass: ScType): Option[ScTemplateDefinition] =
  psiClass.extractClass.flatMap(extractTemplateDefinition)
end extractTemplateDefinition    