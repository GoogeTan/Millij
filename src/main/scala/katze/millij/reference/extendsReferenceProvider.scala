package katze.millij.reference

import cats.Id
import cats.syntax.all.*
import com.intellij.openapi.project.ProjectUtil
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.{PsiFile, PsiReference}
import katze.millij.data.module.NamespacedPath
import katze.millij.data.{ScalaIdentifier, SegmentedPath}
import katze.millij.place.*
import katze.millij.psi.*
import katze.millij.reference.cool.CoolPsiReferenceProvider
import org.jetbrains.yaml.psi.{YAMLMapping, YAMLScalar, YAMLSequence, YAMLSequenceItem}

/**
 * Provides references for extends elements in list case:
 * ```scala
 * object moduleA:
 *  extends: [ScalaModule, PublishModule]
 * ```
 */
def extendsListReferenceProvider : CoolPsiReferenceProvider[
  YAMLScalar,
  YAMLSequenceItem *: YAMLSequence *: YAMLKeyValueWithKey["extends"] *: YAMLMapping  *: YAMLMillModule *: EmptyTuple
] =
  case (scalar, (_, _, _, module, _), _) =>
    makeExtendsReference(scalar, module)
end extendsListReferenceProvider

/**
 * Provides references for extend element
 * ```scala
 * object moduleA:
 *  extends: ScalaModule
 * ```
 */
def extendsValueReferenceProvider : CoolPsiReferenceProvider[
  YAMLExactlyValue[YAMLScalar],
  YAMLKeyValueWithKey["extends"] *: YAMLMapping  *: YAMLMillModule *: EmptyTuple
] =
  case (scalar, (_, module, _), _) =>
    makeExtendsReference(scalar, module)
end extendsValueReferenceProvider

def makeExtendsReference(scalar: YAMLScalar, module : YAMLMapping) : List[PsiReference] =
  val module = enclosingModule(scalar).getOrElse(return Nil)
  ScalaReferenceFactory.makeScalaReferencesFor(scalar, module).toList
end makeExtendsReference

def filesRootModule(file : PsiFile) : Option[SegmentedPath[List, ScalaIdentifier]] =
  psiFilePathRelativeToProjectRoot(file)
    .flatMap(getRelativePathSegments)
    .map(_.dropRight(1))//Dropping build.mill.yaml string.
    .flatMap(_.traverse(ScalaIdentifier.fromStringOption))
    .map(SegmentedPath(_))
end filesRootModule

def psiFilePathRelativeToProjectRoot(psiFile : PsiFile) : Option[String] =
  val virtualFile =
      Option(psiFile.getVirtualFile)
        .orElse(Option(psiFile.getOriginalFile.getVirtualFile))
        .getOrElse(return None)
  val project = psiFile.getProject
  val projectRoot = ProjectUtil.guessProjectDir(project)
  if projectRoot == null then
    return None
  Option(VfsUtilCore.getRelativePath(virtualFile, projectRoot))
end psiFilePathRelativeToProjectRoot

def getRelativePathSegments(relativePath : String): Option[List[String]] =
  if relativePath.isEmpty then
    None
  else
    Some(relativePath.split("/").toList)
  end if
end getRelativePathSegments
