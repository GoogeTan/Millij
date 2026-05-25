package katze.millij

import com.intellij.jarRepository.{RemoteRepositoriesConfiguration, RemoteRepositoryDescription}
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import _root_.kotlin.coroutines.Continuation

import java.nio.file.{Files, Paths}
import scala.jdk.CollectionConverters.*

final class MillProjectStartupActivity extends ProjectActivity:
  override def execute(project: Project, continuation: Continuation[? >: _root_.kotlin.Unit]): AnyRef =
    if isMillProject(project) then
      setupMillRepositories(project)
    null
  end execute

  private def isMillProject(project: Project): Boolean =
    val basePath = project.getBasePath
    if basePath == null then
      false
    else
      val rootDir = Paths.get(basePath)
      Files.exists(rootDir.resolve("build.sc")) ||
        Files.exists(rootDir.resolve("build.mill")) ||
        Files.exists(rootDir.resolve(".mill.yaml"))
    end if
  end isMillProject

  private def setupMillRepositories(project: Project): Unit =
    val config = RemoteRepositoriesConfiguration.getInstance(project)
    val currentRepos = config.getRepositories.asScala.toList

    val hasCentral = currentRepos.exists(_.getUrl.contains("repo1.maven.org/maven2"))

    if !hasCentral then//TODO check me
      val mavenCentral = new RemoteRepositoryDescription(
        "central",
        "Maven Central",
        "https://repo1.maven.org/maven2/"
      )

      val updatedRepos = currentRepos :+ mavenCentral
      config.setRepositories(updatedRepos.asJava)

      println("Mill Plugin: Injected Maven Central into project Remote Jar Repositories.")
  end setupMillRepositories
end MillProjectStartupActivity