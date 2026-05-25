package katze.millij.psi

import org.jetbrains.yaml.psi.*

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
