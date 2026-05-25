package katze.millij.psi

import com.intellij.psi.PsiElement
import org.jetbrains.yaml.psi.YAMLPsiElement


object YAMLGrandChild:
  def unapply(element: PsiElement) : Option[(element.type, YAMLPossibleParent, YAMLPossibleParent)] =
    element match
      case YAMLChild(self, YAMLChild(parent, grandParent)) => Some((element, parent, grandParent))
      case _ => None
    end match
  end unapply
end YAMLGrandChild
