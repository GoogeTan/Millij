package katze.millij.psi

import katze.millij.cool.CoolPattern
import org.jetbrains.yaml.psi.{YAMLKeyValue, YAMLScalar, YAMLSequenceItem}

/**
 * A pattern that matches maven dependency being input in yaml document.
 * The result is the text of the dependency as it is written in the document.
 *
 * It is that complex because yaml is not that intended for things like "org.typelevel::cats-core:x.y.z". It is interpreted
 * as a scalar at the begining:
 * ```YAML
 * org.type<caret>
 * ```
 * then it is treated as key value:
 * ```YAML
 * org.typelevel:<caret>
 * ```
 * then it is treated as a scalar body of key value: //TODO check this fact for :: but it is definitely true for : chains.
 * ```YAML
 * org.typelevel::cats-core<caret>
 * ```
 */
val yamlMavenDependenciesPattern : CoolPattern[String] =
  (
    CoolPattern.elementAndParents[CompletionPosition, (YAMLScalar, YAMLSequenceItem)]()
      .map ((_, scalar, _) => scalar.getText)
      || CoolPattern.elementAndParents[CompletionPosition, (YAMLScalar, YAMLKeyValue, YAMLSequenceItem)]()
      .map((_, scalar, kv, _) => kv.getText)
      || CoolPattern.elementAndParents[CompletionPosition, (YAMLKeyValue, YAMLSequenceItem)]()
      .map((_, kv, _) => kv.getText)
    ).map(cleanElementTextFromDummyIdentifier)
end yamlMavenDependenciesPattern