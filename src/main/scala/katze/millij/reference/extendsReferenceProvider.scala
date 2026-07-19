package katze.millij.reference

import cats.syntax.all.*
import com.intellij.psi.PsiReference
import katze.millij.place.*
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
  case (scalar, (_, _, _, module, _), _) =>
    makeExtendsReference(scalar, module)
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
  case (scalar, (_, module, _), _) =>
    makeExtendsReference(scalar, module)
end extendsValueReferenceProvider

def makeExtendsReference(scalar: YAMLScalar, module : YAMLMapping) : List[PsiReference] =
  val module = enclosingModule(scalar).getOrElse(return Nil)
  ExtendsBlockMemberReference.makeScalaReferencesFor(scalar, module).toList
end makeExtendsReference