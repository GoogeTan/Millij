package katze.millij.completions

import com.intellij.codeInsight.completion.CompletionType
import com.intellij.openapi.diagnostic.Logger
import com.intellij.patterns.{PlatformPatterns, StandardPatterns}
import katze.millij.completions.cool.DumbCoolCompletionContributor
import katze.millij.completions.providers.*
import org.jetbrains.yaml.YAMLLanguage

final class DumbYamlCompletionContributor extends DumbCoolCompletionContributor:
  val logger: Logger = Logger.getInstance(classOf[DumbYamlCompletionContributor])
  coolExtend(
    CompletionType.BASIC,
    extendsExistingKeyCompletionProvider,
    _
      .withLanguage(YAMLLanguage.INSTANCE)
      .inVirtualFile(
        PlatformPatterns.virtualFile().withName(
          StandardPatterns.string().endsWith(".mill.yaml")
        )
      )
  )

  coolExtend(
    CompletionType.BASIC,
    extendsNewKeyCompletionProvider,
    _
      .withLanguage(YAMLLanguage.INSTANCE)
      .inVirtualFile(
        PlatformPatterns.virtualFile().withName(
          StandardPatterns.string().endsWith(".mill.yaml")
        )
      )
  )

  coolExtend(
    CompletionType.BASIC,
    scalaVersionCompletionProvider,
    _
      .withLanguage(YAMLLanguage.INSTANCE)
      .inVirtualFile(
        PlatformPatterns.virtualFile().withName(
          StandardPatterns.string().endsWith(".mill.yaml")
        )
      ),
  )
end DumbYamlCompletionContributor
