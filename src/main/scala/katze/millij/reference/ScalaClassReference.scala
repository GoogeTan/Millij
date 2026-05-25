package katze.millij.reference

import com.intellij.psi.{JavaPsiFacade, PsiElement, PsiReferenceBase}
import com.intellij.psi.search.{GlobalSearchScope, PsiShortNamesCache}

final class ScalaClassReference(element: PsiElement, classNameToSearch: String)
  extends PsiReferenceBase[PsiElement](element):

  override def resolve(): PsiElement =
    val project = getElement.getProject

    // Define where to search. allScope searches project files + libraries.
    // Use projectScope(project) if you only want to resolve to classes in the user's source code.
    val scope = GlobalSearchScope.allScope(project)

    val facade = JavaPsiFacade.getInstance(project)
    val resolvedByFqn = facade.findClass(classNameToSearch, scope)

    if (resolvedByFqn != null) return resolvedByFqn

    val shortNamesCache = PsiShortNamesCache.getInstance(project)
    val classesFound = shortNamesCache.getClassesByName(classNameToSearch, scope)

    classesFound.headOption.orNull

  override def getVariants: Array[AnyRef] =
    Array.empty[AnyRef]
  end getVariants
end ScalaClassReference
