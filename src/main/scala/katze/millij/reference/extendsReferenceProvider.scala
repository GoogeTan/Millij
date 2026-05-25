package katze.millij.reference

import katze.millij.psi.*
import katze.millij.reference.cool.CoolPsiReferenceProvider
import org.jetbrains.yaml.psi.{YAMLMapping, YAMLScalar, YAMLSequence, YAMLSequenceItem}


def extendsListReferenceProvider : CoolPsiReferenceProvider[
  YAMLScalar,
  YAMLSequenceItem *: YAMLSequence *: YAMLKeyValueWithKey["extends"] *: YAMLMapping  *: YAMLMillModule *: EmptyTuple
] =
  case (scalar, _, _) =>
    ScalaReferenceFactory.makeScalaReferencesFor(scalar).toList
end extendsListReferenceProvider

def extendsValueReferenceProvider : CoolPsiReferenceProvider[
  YAMLExactlyValue[YAMLScalar],
  YAMLKeyValueWithKey["extends"] *: YAMLMapping  *: YAMLMillModule *: EmptyTuple
] =
  case (scalar, _, _) =>
    ScalaReferenceFactory.makeScalaReferencesFor(scalar).toList
end extendsValueReferenceProvider