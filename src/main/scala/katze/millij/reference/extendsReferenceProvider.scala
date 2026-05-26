package katze.millij.reference

import katze.millij.psi.*
import katze.millij.reference.cool.CoolPsiReferenceProvider
import org.jetbrains.yaml.psi.{YAMLMapping, YAMLScalar, YAMLSequence, YAMLSequenceItem}

/**
 * Provides references for extends elements in list case:
 * ```scala
 * object moduleA:
 *  extends: [ScalaModule, PublishModule]
 * ```
 */
def extendsListReferenceProvider : CoolPsiReferenceProvider[
  YAMLScalar,
  YAMLSequenceItem *: YAMLSequence *: YAMLKeyValueWithKey["extends"] *: YAMLMapping  *: YAMLMillModule *: EmptyTuple
] =
  case (scalar, _, _) =>
    ScalaReferenceFactory.makeScalaReferencesFor(scalar).toList
end extendsListReferenceProvider

/**
 * Provides references for extend element
 * ```scala
 * object moduleA:
 *  extends: ScalaModule
 * ```
 */
def extendsValueReferenceProvider : CoolPsiReferenceProvider[
  YAMLExactlyValue[YAMLScalar],
  YAMLKeyValueWithKey["extends"] *: YAMLMapping  *: YAMLMillModule *: EmptyTuple
] =
  case (scalar, _, _) =>
    ScalaReferenceFactory.makeScalaReferencesFor(scalar).toList
end extendsValueReferenceProvider