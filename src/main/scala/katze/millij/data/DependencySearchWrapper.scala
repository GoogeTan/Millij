package katze.millij.data

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import org.jetbrains.idea.reposearch.{DependencySearchService, RepositoryArtifactData, SearchParameters}

import java.util.concurrent.{CountDownLatch, TimeUnit}
import java.util.function.Consumer

object DependencySearchWrapper:
  private val Log = Logger.getInstance(classOf[DependencySearchWrapper.type])
  
  //TODO fix me
  def searchCoordinates(
    project: Project,
    query: String,
    onResult: RepositoryArtifactData => Unit
  ): Unit =
    val service = project.getService(classOf[DependencySearchService])
    if service == null then
      return
    end if

    val params = new SearchParameters(true, true)
    val latch = new CountDownLatch(1)

    val consumer = new Consumer[RepositoryArtifactData]:
      override def accept(data: RepositoryArtifactData): Unit =
        // We log the type, just to make sure we're getting something useful
        Log.debug(s"--- Data received: ${data.getClass.getName} (key: ${data.getKey}) ---")
        ProgressManager.checkCanceled()
        onResult(data)

    Log.debug(s"--- Using fulltextSearch for $query ---")
    val promise = service.fulltextSearch(query, params, consumer)

    promise.onSuccess((t: Integer) =>
      Log.debug(s"--- Promise success: $t ---")
      latch.countDown()
    )
    promise.onError((t: Throwable) =>
      Log.debug(s"--- Promise error: ${t.getMessage} ---")
      latch.countDown()
    )

    try
      var count = 0
      while !latch.await(50, TimeUnit.MILLISECONDS) && count < 100 do
        count += 1
        ProgressManager.checkCanceled()
      if (latch.getCount > 0) Log.debug("--- Latch timed out! ---")
    catch
      case _: InterruptedException =>
    end try
  end searchCoordinates

  def searchVersions(
    project: Project,
    groupId: String,
    artifactId: String,
    onResult: RepositoryArtifactData => Unit
  ): Unit =
    val service = project.getService(classOf[DependencySearchService])
    if service == null then
      return

    val params = new SearchParameters(true, true)
    val latch = new CountDownLatch(1)

    val consumer = new Consumer[RepositoryArtifactData]:
      override def accept(data: RepositoryArtifactData): Unit =
        ProgressManager.checkCanceled()
        onResult(data)

    val promise = service.suggestPrefix(groupId, artifactId, params, consumer)

    promise.onSuccess((t: Integer) => latch.countDown())
    promise.onError((t: Throwable) => latch.countDown())

    try
      var attempts = 0
      while attempts < 10 && !latch.await(50, TimeUnit.MILLISECONDS) do
        ProgressManager.checkCanceled()
        attempts += 1
      end while
    catch
      case _: InterruptedException =>
    end try
  end searchVersions

  def extractParts(data: RepositoryArtifactData): (String, String, String) =
    val parts = data.getKey.split(":")
    val g = if (parts.length > 0) parts(0) else ""
    val a = if (parts.length > 1) parts(1) else ""
    val v = if (parts.length > 2) parts(2) else ""
    (g, a, v)
  end extractParts
end DependencySearchWrapper