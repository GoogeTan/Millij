package katze.millij
import cats.syntax.all.*
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.project.{Project, ProjectUtil}
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.{EditorNotificationPanel, EditorNotificationProvider}
import katze.millij.externalSystem.MillRunner

import java.util.function.Function
import javax.swing.JComponent

final class BspImportNotificationProvider extends EditorNotificationProvider:
  override def collectNotificationData(
    project: Project,
    file: VirtualFile
  ): Function[? >: FileEditor, ? <: JComponent] =
    val result = for
      ext <- Option(file.getName).filter(_.endsWith(".mill.yaml"))
      baseDir <- Option(ProjectUtil.guessProjectDir(project))
      bspPath = baseDir.findChild(".bsp")
      if bspPath == null || !bspPath.exists()
    yield
      (editor: FileEditor) => createPanel(project, baseDir, editor)

    result.fold[Function[? >: FileEditor, ? <: JComponent] | Null](null)(f => editor => f(editor))
  end collectNotificationData

  private def createPanel(project: Project, baseDir: VirtualFile, editor: FileEditor): EditorNotificationPanel =
    val panel = EditorNotificationPanel(EditorNotificationPanel.Status.Info)
    panel.setText("Mill project configuration detected.")
    panel.createActionLabel("Load Mill Project", () => MillRunner.installAndRefreshBsp(project, baseDir))

    panel
  end createPanel
end BspImportNotificationProvider