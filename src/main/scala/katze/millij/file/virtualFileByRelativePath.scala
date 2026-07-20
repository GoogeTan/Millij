package katze.millij.file

import cats.{Foldable, Show}
import com.intellij.openapi.project.{Project, ProjectUtil}
import com.intellij.openapi.vfs.VirtualFile
import cats.syntax.all.*
import katze.millij.path.SegmentedPath

def virtualFileByRelativePath(project: Project, relativePath: String) : Option[VirtualFile] =
  for
    baseDir <- Option(ProjectUtil.guessProjectDir(project))
    file <- Option(baseDir.findFileByRelativePath(relativePath))
  yield file
end virtualFileByRelativePath

def virtualFileByRelativePath[F[_] : Foldable, T : Show](
  project: Project,
  relativePath: SegmentedPath[F, T]
) : Option[VirtualFile] =
  virtualFileByRelativePath(project, relativePath.asFilePath)
end virtualFileByRelativePath
