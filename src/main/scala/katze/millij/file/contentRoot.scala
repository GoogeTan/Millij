package katze.millij.file

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.{VfsUtilCore, VirtualFile}
import com.intellij.psi.PsiFile
import com.intellij.util.indexing.FileContent

extension (value: FileContent)
  def contentRoot: Option[VirtualFile] =
    value.getFile.contentRoot(value.getProject)
  end contentRoot

  def relativePathToContentRoot: Option[String] =
    value.getFile.relativePathToContentRoot(value.getProject)
  end relativePathToContentRoot
end extension

extension (value: PsiFile)
  def contentRoot: Option[VirtualFile] =
    value.getOriginalFile.getVirtualFile.contentRoot(value.getProject)
  end contentRoot

  def relativePathToContentRoot: Option[String] =
    value.getOriginalFile.getVirtualFile.relativePathToContentRoot(value.getProject)
  end relativePathToContentRoot
end extension


extension (file: VirtualFile)
  def contentRoot(project : Project): Option[VirtualFile] =
    if project == null || project.isDisposed || file == null then
      return None
    end if
    val fileIndex = ProjectFileIndex.getInstance(project)
    Option(fileIndex.getContentRootForFile(file))// TODO this requires read lock
  end contentRoot

  def relativePathToContentRoot(project : Project): Option[String] =
    file.contentRoot(project).flatMap(contentRoot =>
      Option(VfsUtilCore.getRelativePath(file, contentRoot))
    )
  end relativePathToContentRoot
end extension