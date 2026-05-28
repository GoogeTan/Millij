package katze.millij.reference

import com.intellij.codeInsight.highlighting.TooltipLinkHandler
import com.intellij.openapi.editor.Editor
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope

class ScTypeNavigationHandler extends TooltipLinkHandler:
  override def handleLink(refSuffix: String, editor: Editor): Boolean =
    val project = editor.getProject
    if project == null then
      return false
    end if

    val facade = JavaPsiFacade.getInstance(project)
    val psiClass = facade.findClass(refSuffix, GlobalSearchScope.allScope(project))
    if psiClass != null then
      psiClass.navigate(true)
      true
    else
      false
    end if
  end handleLink
end ScTypeNavigationHandler