package katze.millij.psi

import org.jetbrains.yaml.psi.{YAMLDocument, YAMLFile, YAMLPsiElement}

type YAMLPossibleParent = YAMLPossibleNonFileParent | YAMLFile | YAMLDocument

object YAMLPossibleParent:
  def unapply(value : YAMLPsiElement) : Option[YAMLPossibleParent] =
    value match
      case YAMLPossibleNonFileParent(value) =>
        Some(value)
      case document: YAMLDocument => Some(document)
      case file: YAMLFile => Some(file)
      case _ => None
    end match
  end unapply
end YAMLPossibleParent
