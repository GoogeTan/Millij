package katze.millij.data

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.*
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.JDOMUtil
import com.intellij.util.io.HttpRequests

import java.util
import java.util.concurrent.atomic.AtomicBoolean
import scala.beans.BeanProperty
import scala.jdk.CollectionConverters.*

class ScalaVersionState:
  @BeanProperty
  var versions: java.util.List[String] = new java.util.ArrayList[String]()
  @BeanProperty
  var lastFetched: Long = 0L

@Service(Array(Service.Level.APP))
@State(
  name = "ScalaVersionCache",
  storages = Array(new Storage("scalaVersions.xml"))
)
final class ScalaVersionCacheService extends PersistentStateComponent[ScalaVersionState]:
  private val log = Logger.getInstance(classOf[ScalaVersionCacheService])

  @volatile private var myState = new ScalaVersionState()
  private val isFetching = new AtomicBoolean(false)
  private val CacheTTL: Long = 24 * 60 * 60 * 1000L
  private val defaultVersions = List("3.3.3", "3.3.1", "2.13.14")

  override def getState: ScalaVersionState = myState

  override def loadState(state: ScalaVersionState): Unit =
    myState = state

  def getVersions: List[String] =
    val now = System.currentTimeMillis()
    val timeSinceLastFetch = now - myState.lastFetched

    if timeSinceLastFetch > CacheTTL then
      if isFetching.compareAndSet(false, true) then
        fetchVersionsAsync()

    val cached = myState.versions.asScala.toList
    if cached.nonEmpty then
      cached
    else
      defaultVersions
  end getVersions

  private def fetchVersionsAsync(): Unit =
    ApplicationManager.getApplication.executeOnPooledThread(new Runnable:
      override def run(): Unit =
        try
          val scala3Url = "https://repo1.maven.org/maven2/org/scala-lang/scala3-library_3/maven-metadata.xml"
          val scala2Url = "https://repo1.maven.org/maven2/org/scala-lang/scala-library/maven-metadata.xml"

          val versions3 = parseMavenMetadata(scala3Url)
          val versions2 = parseMavenMetadata(scala2Url)

          val allVersions = versions3 ++ versions2
          val filteredVersions = allVersions.filterNot(v => v.contains("-RC") || v.contains("-M"))

          if filteredVersions.nonEmpty then
            val newState = new ScalaVersionState()
            newState.versions = new util.ArrayList(filteredVersions.asJava)
            newState.lastFetched = System.currentTimeMillis()
            myState = newState
          end if
        catch
          case e: Exception =>
            log.warn("Fatal error occurred while fetching Scala versions from Maven Central", e)
        finally
          isFetching.set(false)
      )
  end fetchVersionsAsync

  private def parseMavenMetadata(url: String): List[String] =
    try
      val document = HttpRequests.request(url).connect { request =>
        JDOMUtil.load(request.getInputStream)
      }

      val versioning = document.getChild("versioning")
      if versioning != null then
        val versions = versioning.getChild("versions")
        if versions != null then
          versions.getChildren("version").asScala.map(_.getText).toList
        else
          List.empty
      else
        List.empty
    catch
      case e: Exception =>
        List.empty

object ScalaVersionCacheService:
  def getInstance: ScalaVersionCacheService =
    ApplicationManager.getApplication.getService(classOf[ScalaVersionCacheService])
  end getInstance
end ScalaVersionCacheService