package katze.millij.data

import com.intellij.codeInsight.completion.*
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.project.Project
import katze.millij.*
import katze.millij.data.DependencySearchWrapper
import org.jetbrains.idea.reposearch.RepositoryArtifactData

import scala.jdk.CollectionConverters.*

object MavenDependencyShared:
  def searchAndSuggestDependencies(
    resultSet: CompletionResultSet,
    scalaVersion: String,
    project: Project,
    fullText: String
  ): Unit =
    val adjustedResultSet = resultSet.withPrefixMatcher(
      new PrefixMatcher(fullText):
        override def prefixMatches(name: String): Boolean = true

        override def cloneWithPrefix(newPrefix: String): PrefixMatcher = this
    )
    val dep = PartialMillDependency.parse(fullText)
    val searchArtifactId = dep.artifactId.getOrElse("")

    println(s"--- Dependency Suggestion ---")
    println(s"  * Prefix Matcher: '$fullText'")
    println(s"  * Parsed Request -> Group: '${dep.groupId}', Artifact: '$searchArtifactId'")

    DependencySearchWrapper.searchCoordinates(
      project,
      s"${dep.groupId}${dep.artifactId.fold("")(t => s":${t}")}",
      (t: RepositoryArtifactData) =>
        println(s"  * Received result: ${t.getClass.getName}")
        if !t.getClass.getSimpleName.contains("Poisoned") then
          try
            val getGroupIdMethod = t.getClass.getMethod("getGroupId")
            val getArtifactIdMethod = t.getClass.getMethod("getArtifactId")
            val getItemsMethod = t.getClass.getMethod("getItems")

            val groupId = getGroupIdMethod.invoke(t).asInstanceOf[String]
            val artifactId = getArtifactIdMethod.invoke(t).asInstanceOf[String]

            val itemsRaw = getItemsMethod.invoke(t)
            val items: Iterable[Any] = itemsRaw match
              case arr: Array[_] => arr.toList
              case coll: java.util.Collection[_] => coll.asScala
              case _ => Iterable.empty

            // 1. Calculate the Scala suffixes for your current project
            val binarySuffix = s"_${dep.getScalaBinaryVersion(scalaVersion)}"
            val fullSuffix = s"_$scalaVersion"

            var finalScalaSep = ":"
            var finalBaseArtifactId = artifactId

            // 2. Smart Formatting Logic
            dep.scalaVersioning match
              case Some(ScalaVersioning.ScalaBinary) =>
                // User EXPLICITLY typed `::`. Strictly filter out non-matching artifacts.
                if artifactId == null || !artifactId.endsWith(binarySuffix) then return
                finalScalaSep = "::"
                finalBaseArtifactId = artifactId.stripSuffix(binarySuffix)
              case Some(ScalaVersioning.ScalaFull) =>
                // User EXPLICITLY typed `:::`. Strictly filter out non-matching artifacts.
                if artifactId == null || !artifactId.endsWith(fullSuffix) then return
                finalScalaSep = ":::"
                finalBaseArtifactId = artifactId.stripSuffix(fullSuffix)
              case _ =>
                // User hasn't typed colons yet. Auto-detect from the Maven database!
                if artifactId != null then
                  if artifactId.endsWith(fullSuffix) then
                    finalScalaSep = ":::"
                    finalBaseArtifactId = artifactId.stripSuffix(fullSuffix)
                  else if artifactId.endsWith(binarySuffix) then
                    finalScalaSep = "::"
                    finalBaseArtifactId = artifactId.stripSuffix(binarySuffix)

            val platformSep = dep.platformVersion.map {
              case PlatformVersion.Standard => ":"
              case PlatformVersion.NativeJS => "::"
            }.getOrElse(":")

            // 3. Apply the suggestions
            if items.isEmpty then
              val suggestion = if finalBaseArtifactId != null && finalBaseArtifactId.nonEmpty then s"$groupId$finalScalaSep$finalBaseArtifactId" else groupId
              if suggestion != null && suggestion.nonEmpty then
                adjustedResultSet.addElement(
                  LookupElementBuilder
                    .create(suggestion)
                    .withTypeText("mill dependency")
                    .withIcon(AllIcons.Nodes.PpLib)
                )
            else
              for item <- items do
                val getVersionMethod = item.getClass.getMethod("getVersion")
                val version = getVersionMethod.invoke(item).asInstanceOf[String]
                if version != null then
                  val suggestion = s"$groupId$finalScalaSep$finalBaseArtifactId$platformSep$version"
                  adjustedResultSet.addElement(
                    LookupElementBuilder
                      .create(suggestion)
                      .withTypeText("mill dependency")
                      .withIcon(AllIcons.Nodes.PpLib)
                  )
          catch
            case e: NoSuchMethodException =>
              println(s"  * ERROR NoSuchMethodException: ${e.getMessage} on class ${t.getClass.getName}")
            case e: Exception => println(s"  * ERROR parsing artifact data: ${e.getMessage}")
    )
end MavenDependencyShared
