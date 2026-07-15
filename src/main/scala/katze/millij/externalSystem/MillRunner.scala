package katze.millij.externalSystem

import cats.syntax.all.*
import com.intellij.execution.configurations.{GeneralCommandLine, PathEnvironmentVariableUtil}
import com.intellij.execution.process.OSProcessHandler
import com.intellij.notification.{NotificationGroupManager, NotificationType}
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.{ProgressIndicator, ProgressManager, Task}
import com.intellij.openapi.project.{Project, ProjectManager}
import com.intellij.openapi.vfs.{VfsUtil, VirtualFile}

object MillRunner:

  def installAndRefreshBsp(project: Project, baseDir: VirtualFile): Unit =
    ProgressManager.getInstance().run(
      new Task.Backgroundable(project, "Installing Mill BSP...", false):
        override def run(indicator: ProgressIndicator): Unit =
          findMillExecutable(baseDir) match
            case Some(millExe) => executeMill(millExe, project, baseDir)
            case None => reportError(project, "Could not find 'mill' executable locally or globally in PATH.")
    )
  end installAndRefreshBsp

  private def findMillExecutable(baseDir: VirtualFile): Option[String] =
    val localPath = baseDir.toNioPath.resolve("mill").toFile
    val localMill = Option.when(localPath.exists() && localPath.canExecute)(localPath.getAbsolutePath)
    lazy val globalMill = Option(PathEnvironmentVariableUtil.findInPath("mill")).map(_.getAbsolutePath)
    localMill.orElse(globalMill)
  end findMillExecutable

  private def executeMill(millExe: String, project: Project, baseDir: VirtualFile): Unit =
    try
      val cmd = GeneralCommandLine(millExe, "mill.bsp.BSP/install")
      cmd.setWorkDirectory(baseDir.getPath)
      val handler = OSProcessHandler(cmd)
      handler.startNotify()
      handler.waitFor()

      if handler.getExitCode == 0 then
        VfsUtil.markDirtyAndRefresh(false, true, true, baseDir)
        ApplicationManager.getApplication.invokeLater(() =>
          ProjectManager.getInstance().reloadProject(project)
        )
      else
        reportError(project, s"Mill BSP installation failed with exit code: ${handler.getExitCode}")
    catch
      case e: Exception =>
        reportError(project, s"Exception occurred while running Mill: ${e.getMessage}")
  end executeMill

  private def reportError(project: Project, message: String): Unit =
    NotificationGroupManager.getInstance()
      .getNotificationGroup("Mill BSP Notifications")
      .createNotification("BSP Import Failed", message, NotificationType.ERROR)
      .notify(project)
  end reportError

end MillRunner