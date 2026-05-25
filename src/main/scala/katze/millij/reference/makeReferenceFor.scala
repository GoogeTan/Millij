package katze.millij.reference

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.{NavigatablePsiElement, PsiElement, PsiReference, PsiReferenceBase}
import katze.millij.{yamlDefinableMembersOfScope, richScopeOf}
import katze.millij.psi.CompletionPosition
import org.jetbrains.yaml.psi.{YAMLKeyValue, YAMLPsiElement, YAMLScalar}

def makeReferenceFor(psiElement : YAMLScalar | YAMLKeyValue): PsiReference =
  val nameToSearch = psiElement match
    case sc: YAMLScalar => sc.getTextValue
    case kv: YAMLKeyValue => kv.getKeyText
  YamlMemberReference(psiElement, nameToSearch)
end makeReferenceFor

final class YamlMemberReference(element: YAMLPsiElement | CompletionPosition, nameToSearch: String) extends PsiReferenceBase[PsiElement](element):
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