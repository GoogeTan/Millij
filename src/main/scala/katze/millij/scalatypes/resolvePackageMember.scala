package katze.millij.scalatypes

import com.intellij.psi.{JavaPsiFacade, PsiPackage}
import com.intellij.psi.search.GlobalSearchScope
import katze.millij.data.{ResolvedSymbol, ScalaIdentifier, Smart}
import org.jetbrains.plugins.scala.lang.psi.impl.ScalaPsiManager

def resolvePackageMember(pkg : PsiPackage, memberName : ScalaIdentifier, scope: GlobalSearchScope)(using Smart) : List[ResolvedSymbol[ScalaIdentifier]] =
  val project = pkg.getProject
  val parentFqn = pkg.getQualifiedName

  val targetFqn =
    if parentFqn.isEmpty then memberName.asString
    else s"$parentFqn.${memberName.asString}"

  val classesAndObjects =
    ScalaPsiManager.instance(project)
      .getCachedClasses(scope, targetFqn)
      .toList
      .flatMap(ResolvedSymbol.fromPsiElement[ScalaIdentifier])

  val packageObjects = ScalaPsiManager.instance(project).getPackageImplicitObjects(parentFqn, scope).toList

  val innerMembers = for
    pkgObj <- packageObjects
    member <- pkgObj.members.filter(_.getName == memberName.asString)
    resolvedMember <- ResolvedSymbol.fromPsiElement[ScalaIdentifier](member)
  yield resolvedMember

  val packageOpt = Option(
    JavaPsiFacade.getInstance(project).findPackage(targetFqn)
  ).map(ResolvedSymbol.ScalaPackage[ScalaIdentifier](_))

  classesAndObjects ++ innerMembers ++ packageOpt.toList
end resolvePackageMember
