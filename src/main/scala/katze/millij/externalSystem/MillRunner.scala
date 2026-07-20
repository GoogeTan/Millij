package katze.millij.externalSystem

import cats.syntax.all.*
import com.intellij.build.DefaultBuildDescriptor
import com.intellij.build.events.{Failure, FailureResult, FinishBuildEvent, OutputBuildEvent, StartBuildEvent, SuccessResult, Warning}
import com.intellij.execution.configurations.{GeneralCommandLine, PathEnvironmentVariableUtil}
import com.intellij.execution.process.{OSProcessHandler, ProcessEvent, ProcessListener, ProcessOutputType}
import com.intellij.notification.{NotificationGroupManager, NotificationType}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder
import com.intellij.openapi.externalSystem.model.ProjectSystemId
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.progress.{ProgressIndicator, ProgressManager, Task}
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.{VfsUtil, VirtualFile}

import java.util
import java.util.Collections

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

      val buildId = new Object()
      val syncViewManager = project.getService(classOf[com.intellij.build.SyncViewManager])
      val buildDescriptor = DefaultBuildDescriptor(
        buildId,
        "Mill BSP Setup",
        baseDir.getPath,
        System.currentTimeMillis()
      )

      val startEvent = StartBuildEvent.builder(buildId.toString, buildDescriptor).build()
      syncViewManager.onEvent(buildId, startEvent)

      handler.addProcessListener(new ProcessListener:
        override def onTextAvailable(event: ProcessEvent, outputType: Key[?]): Unit =
          outputType match
            case processOutputType : ProcessOutputType =>
              val outputEvent = OutputBuildEvent.builder(event.getText)
                .withParentId(buildId)
                .withOutputType(processOutputType)
                .build()
              syncViewManager.onEvent(buildId, outputEvent)
            case _ => ()
      )

      handler.startNotify()
      handler.waitFor()

      if handler.getExitCode == 0 then
        val successResult = new SuccessResult:
          override def isUpToDate: Boolean = false
          override def getWarnings: util.List[? <: Warning] = Collections.emptyList()

        val finishEvent = FinishBuildEvent.builder(
          buildId,
          "Success",
          successResult
        ).build()
        syncViewManager.onEvent(buildId, finishEvent)

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
        val failureResult = new FailureResult:
          override def getFailures: util.List[? <: Failure] = Collections.emptyList()

        val finishEvent = FinishBuildEvent.builder(
          buildId,
          "Failed",
          failureResult
        ).build()
        syncViewManager.onEvent(buildId, finishEvent)
        reportError(project, s"Mill BSP installation failed with exit code: ${handler.getExitCode}")
        onFinishedCallback.foreach(callback =>
          ApplicationManager.getApplication.invokeLater(() => callback(false))
        )
    catch
      case e: Exception =>
        reportError(project, s"Exception occurred while running Mill: ${e.getMessage}")
        onFinishedCallback.foreach(callback =>
          ApplicationManager.getApplication.invokeLater(() => callback(false))
        )
  end executeMill

  private def reportError(project: Project, message: String): Unit =
    NotificationGroupManager.getInstance()
      .getNotificationGroup("Mill BSP Notifications")
      .createNotification("BSP Import Failed", message, NotificationType.ERROR)
      .notify(project)
  end reportError
end MillRunner