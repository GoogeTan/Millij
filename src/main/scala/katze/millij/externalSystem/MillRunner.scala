package katze.millij.externalSystem

import cats.syntax.all.*
import com.intellij.execution.configurations.{GeneralCommandLine, PathEnvironmentVariableUtil}
import com.intellij.execution.process.OSProcessHandler
import com.intellij.notification.{NotificationGroupManager, NotificationType}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.progress.{ProgressIndicator, ProgressManager, Task}
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.{VfsUtil, VirtualFile}

object MillRunner:

  def installAndRefreshBsp(
    project: Project,
    baseDir: VirtualFile,
    onFinishedCallback: Option[Boolean => Unit] = None
  ): Unit =
    ProgressManager.getInstance().run(
      new Task.Backgroundable(project, "Installing Mill BSP...", false):
        override def run(indicator: ProgressIndicator): Unit =
          findMillExecutable(baseDir) match
            case Some(millExe) => executeMill(millExe, project, baseDir, onFinishedCallback)
            case None =>
              reportError(project, "Could not find 'mill' executable locally or globally in PATH.")
              onFinishedCallback.foreach(_(false))
    )
  end installAndRefreshBsp

  def findMillExecutable(baseDir: VirtualFile): Option[String] =
    val isWin = com.intellij.openapi.util.SystemInfo.isWindows
    val name = if isWin then "mill.bat" else "mill"
    val localPath = baseDir.toNioPath.resolve(name).toFile
    val localMill = Option.when(localPath.exists() && (isWin || localPath.canExecute))(localPath.getAbsolutePath)
    lazy val globalMill = Option(PathEnvironmentVariableUtil.findInPath(name)).map(_.getAbsolutePath)

    localMill <+> globalMill
  end findMillExecutable

  private def executeMill(
    millExe: String,
    project: Project,
    baseDir: VirtualFile,
    onFinishedCallback: Option[Boolean => Unit]
  ): Unit =
    try
      val cmd = GeneralCommandLine(millExe, "mill.bsp.BSP/install")
      cmd.setWorkDirectory(baseDir.getPath)
      val handler = OSProcessHandler(cmd)

      // 1. Initialize the Sync tool window integration
      val buildId = new Object()
      val syncViewManager = project.getService(classOf[com.intellij.build.SyncViewManager])
      val buildDescriptor = com.intellij.build.DefaultBuildDescriptor(
        buildId,
        "Mill BSP Setup",
        baseDir.getPath,
        System.currentTimeMillis()
      )

      // Notify the IDE that a sync process has started
      syncViewManager.onEvent(buildId, com.intellij.build.events.impl.StartBuildEventImpl(buildDescriptor, "Installing Mill BSP..."))

      // 2. Intercept process output and pipe it to the Sync view
      handler.addProcessListener(new com.intellij.execution.process.ProcessAdapter:
        override def onTextAvailable(event: com.intellij.execution.process.ProcessEvent, outputType: com.intellij.openapi.util.Key[?]): Unit =
          val isStdOut = outputType == com.intellij.execution.process.ProcessOutputTypes.STDOUT
          // This streams the CLI text directly into the Build tool window console
          syncViewManager.onEvent(buildId, com.intellij.build.events.impl.OutputBuildEventImpl(buildId, event.getText, isStdOut))
      )

      handler.startNotify()
      handler.waitFor()

      // 3. Mark the sync process as finished (Success or Failure)
      if handler.getExitCode == 0 then
        syncViewManager.onEvent(buildId, com.intellij.build.events.impl.FinishBuildEventImpl(
          buildId, null, System.currentTimeMillis(), "Success", com.intellij.build.events.impl.SuccessResultImpl()
        ))

        VfsUtil.markDirtyAndRefresh(false, true, true, baseDir)
        ApplicationManager.getApplication.invokeLater: () =>
          val bspId = ProjectSystemId("BSP")
          val importSpec = ImportSpecBuilder(project, bspId).build()

          ExternalSystemUtil.refreshProject(
            baseDir.getPath,
            importSpec
          )
          onFinishedCallback.foreach(_(true))
      else
        syncViewManager.onEvent(buildId, com.intellij.build.events.impl.FinishBuildEventImpl(
          buildId, null, System.currentTimeMillis(), "Failed", com.intellij.build.events.impl.FailureResultImpl()
        ))
        reportError(project, s"Mill BSP installation failed with exit code: ${handler.getExitCode}")
        onFinishedCallback.foreach(_(false))
    catch
      case e: Exception =>
        reportError(project, s"Exception occurred while running Mill: ${e.getMessage}")
        onFinishedCallback.foreach(_(false))
  end executeMill

  private def reportError(project: Project, message: String): Unit =
    NotificationGroupManager.getInstance()
      .getNotificationGroup("Mill BSP Notifications")
      .createNotification("BSP Import Failed", message, NotificationType.ERROR)
      .notify(project)
  end reportError

end MillRunner