package katze.millij.reference

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.{NavigatablePsiElement, PsiElement, PsiReference, PsiReferenceBase}
import katze.millij.place.{yamlDefinableMembersOfScope, richScopeOf}
import katze.millij.psi.CompletionPosition
import org.jetbrains.yaml.psi.{YAMLKeyValue, YAMLPsiElement, YAMLScalar}

/**
 * Creates a reference for a given member of a YAML file.
 * * Example:
 * ```yaml
 * extends: [ScalaModule, PublishModule]
 *
 * pomSettings:
 *  developers:
 * ```
 * 
 * - If the function is called for `pomSettings`, it will search for a member named `pomSettings`
 * within `ScalaModule` and `PublishModule`, and create a reference to it.
 * - If the function is called for `developers`, it will first resolve `pomSettings` (as described above), 
 * then search for the `developers` member inside that result, and create a reference to it.
 *
 * @param psiElement The YAML scalar or key-value element to create a reference for.
 * @return A PsiReference pointing to the resolved member, or pointing to null if unresolved.
 */
def makeReferenceFor(psiElement : YAMLScalar | YAMLKeyValue): PsiReference =
  val nameToSearch = psiElement match
    case sc: YAMLScalar => sc.getTextValue
    case kv: YAMLKeyValue => kv.getKeyText
  YamlMemberReference(psiElement, nameToSearch)
end makeReferenceFor

final class YamlMemberReference(element: YAMLPsiElement, nameToSearch: String) extends PsiReferenceBase[PsiElement](element):
  override def resolve(): PsiElement =
    richScopeOf(element)
      .map(yamlDefinableMembersOfScope)
      .fold(
        _ => null,
        variants =>
          variants
            .find(_.name == nameToSearch)
            .map(_.getNavigationElement)
            .orNull
      )
  end resolve

  override def getVariants: Array[AnyRef] =
    richScopeOf(element)
      .map(yamlDefinableMembersOfScope)
      .fold(
        _ => Array.empty[AnyRef],
        variants =>
          variants.map { variant =>
            LookupElementBuilder.create(variant.name)
              .withIcon(AllIcons.Nodes.Method)
              .withTypeText("Method")
          }.toArray
      )
  end getVariants
end YamlMemberReference