package katze.millij.reference

import com.intellij.patterns.{PlatformPatterns, StandardPatterns}
import com.intellij.psi.*
import org.jetbrains.yaml.psi.*
import katze.millij.reference.cool.registerCoolReferenceProvider
import katze.millij.*

class YamlReferenceContributor extends PsiReferenceContributor:
  override def registerReferenceProviders(registrar: PsiReferenceRegistrar): Unit =
    registrar.registerCoolReferenceProvider[YAMLScalar, YAMLKeyValue *: YAMLMapping *: EmptyTuple](
      {
        case (currentElement, kv *: _, context) =>
          if !isObjectDeclarationText(kv.getKeyText) then
            List(makeReferenceFor(currentElement))
          else
            Nil
      },
      _.inVirtualFile(
        PlatformPatterns.virtualFile().withName(
          StandardPatterns.string().endsWith(".mill.yaml")
        )
      )
    )

    registrar.registerCoolReferenceProvider(
      extendsValueReferenceProvider,
      _.inVirtualFile(
        PlatformPatterns.virtualFile().withName(
          StandardPatterns.string().endsWith(".mill.yaml")
        )
      )
    )

    registrar.registerCoolReferenceProvider(
      extendsListReferenceProvider,
      _.inVirtualFile(
        PlatformPatterns.virtualFile().withName(
          StandardPatterns.string().endsWith(".mill.yaml")
        )
      )
    )

    registrar.registerCoolReferenceProvider[YAMLKeyValue, YAMLMapping](
      (currentElement, _, context) =>
        if !isObjectDeclarationText(currentElement.getKeyText) then
          List(makeReferenceFor(currentElement))
        else
          Nil,
      _.inVirtualFile(
        PlatformPatterns.virtualFile().withName(
          StandardPatterns.string().endsWith(".mill.yaml")
        )
      )
    )
  end registerReferenceProviders
end YamlReferenceContributor

