package katze.millij.scalatypes

import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import katze.millij.data.Smart
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.ScTemplateDefinition
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager
import org.jetbrains.plugins.scala.lang.psi.types.{ScType, ScTypeExt, extractTypeParameters}
import org.jetbrains.plugins.scala.lang.psi.types.api.designator.ScDesignatorType


def extractTemplateDefinition(psiClass: PsiClass)(using Smart): Option[ScTemplateDefinition] =
  psiClass match
    case templateDef: ScTemplateDefinition => Some(templateDef)
    case _ => None
end extractTemplateDefinition

def extractTemplateDefinition(psiClass: ScType)(using Smart): Option[ScTemplateDefinition] =
  psiClass.extractClass.flatMap(extractTemplateDefinition)
end extractTemplateDefinition    