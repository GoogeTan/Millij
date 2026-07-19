package katze.millij.scalatypes

import com.intellij.psi.{JavaPsiFacade, PsiPackage}
import com.intellij.psi.search.GlobalSearchScope
import katze.millij.data.{ResolvedSymbol, Smart}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager

def resolvePackageMember(pkg : PsiPackage, memberName : String, scope: GlobalSearchScope)(using Smart) : List[ResolvedSymbol[String]] =
  val project = pkg.getProject
  val parentFqn = pkg.getQualifiedName

  val targetFqn =
    if parentFqn.isEmpty then memberName
    else s"$parentFqn.${memberName}"

  val classesAndObjects =
    ScalaPsiManager.instance(project)
      .getCachedClasses(scope, targetFqn)
      .toList
      .flatMap(ResolvedSymbol.fromPsiElement[String])

  val packageObjects = ScalaPsiManager.instance(project).getPackageImplicitObjects(parentFqn, scope).toList

  val innerMembers = for
    pkgObj <- packageObjects
    member <- pkgObj.members.filter(_.getName == memberName)
    resolvedMember <- ResolvedSymbol.fromPsiElement[String](member)
  yield resolvedMember

  val packageOpt = Option(
    JavaPsiFacade.getInstance(project).findPackage(targetFqn)
  ).map(ResolvedSymbol.ScalaPackage[String](_))

  classesAndObjects ++ innerMembers ++ packageOpt.toList
end resolvePackageMember
