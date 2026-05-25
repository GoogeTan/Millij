package katze.millij.reference.cool

import com.intellij.psi.PsiReference
import com.intellij.util.ProcessingContext

type CoolPsiReferenceProvider[Element, Parents] = (Element, Parents, ProcessingContext) => List[PsiReference]
