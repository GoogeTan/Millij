package katze.millij.psi

import org.jetbrains.yaml.psi.YAMLPsiElement

object YAMLChild:
  def unapply(element: YAMLPsiElement | CompletionPosition) : Option[(element.type, YAMLPossibleParent)] =
    element.getParent match
      case YAMLPossibleParent(parent) => Some((element, parent))
      case _ => None
    end match
  end unapply
end YAMLChild