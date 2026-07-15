package katze.millij.reference

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.psi.{PsiElement, PsiReferenceBase}
import katze.millij.data.Smart
import katze.millij.place.{richPlaceOf, yamlDefinableMembersOfScope}
import org.jetbrains.yaml.psi.YAMLPsiElement

final class YamlMemberReference(element: YAMLPsiElement, nameToSearch: String) extends PsiReferenceBase[PsiElement](element):
  override def resolve(): PsiElement =
    Smart(element.getProject) {
      richPlaceOf(element)
        .map(yamlDefinableMembersOfScope)
        .fold(
          _ => null,
          variants =>
            variants
              .find(_.name == nameToSearch)
              .map(_.getNavigationElement)
              .orNull
        )
    }.orNull
  end resolve

  override def getVariants: Array[AnyRef] =
    Smart(element.getProject) {
      richPlaceOf(element)
        .map(yamlDefinableMembersOfScope)
        .fold(
          _ => Array.empty[AnyRef],
          variants =>
            variants.map { variant =>
              LookupElementBuilder.create(variant.name)
                .withIcon(AllIcons.Nodes.Method)
                .withTypeText("Method")
            }.toArray[AnyRef]
        )
    }.getOrElse[Array[AnyRef]](Array[AnyRef]())
  end getVariants
end YamlMemberReference
