package katze.millij.psi

import org.jetbrains.yaml.psi.*

/**
 * A type of YAMLPsiElement.getParent in case it happened not to be a file or whole document.
 */
type YAMLPossibleNonFileParent = YAMLMapping | YAMLSequence | YAMLSequenceItem | YAMLKeyValue | YAMLScalar | YAMLAnchor | YAMLAlias

object YAMLPossibleNonFileParent:
  def unapply(value : YAMLPsiElement) : Option[YAMLPossibleNonFileParent] =
    value match
      case nonRoot @ (
        _: YAMLMapping | _: YAMLSequence | _: YAMLSequenceItem | _: YAMLKeyValue | _: YAMLScalar | _: YAMLAlias | _ : YAMLAnchor
      ) =>
        Some(nonRoot)
      case _ => None
    end match
  end unapply
end YAMLPossibleNonFileParent
