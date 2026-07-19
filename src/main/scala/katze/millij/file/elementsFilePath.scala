package katze.millij.file

import com.intellij.psi.PsiElement
import katze.millij.data.SegmentedPath

/**
 * Relative location of a file to which element belongs.
 *
 * It is the same as a name of root module in a file in which element is.
 *
 * @param element Relative path or error.
 * @return
 */
def elementsFilePath(element : PsiElement) : Either[String, SegmentedPath[List, String]] =
  val filePathRelativeToProjectRoot = element
    .getContainingFile.relativePathToContentRoot.toRight("Couldn't determine a file location relative to the project root")
  filePathRelativeToProjectRoot
    .map(SegmentedPath.folderPath)
end elementsFilePath