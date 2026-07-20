package katze.millij.scalatypes

import cats.*
import cats.arrow.FunctionK
import cats.data.*
import cats.syntax.all.*
import com.intellij.psi.PsiClass
import katze.millij.data.nonEmptyListToSemigroupK
import katze.millij.path.SegmentedPath

/**
 * Those imports are added by mill on top of every YAML config implicitly.
 */
val imports : List[SegmentedPath[NonEmptyList, String]] =
  List(
    "mill", "mill.scalalib", "mill.javalib", "mill.kotlinlib"
  )
    .map(SegmentedPath.fromQualifiedNonEmpty(_).get)

/**
 * Makes a shortened version of the FQN which accounts for the present imports
 */
def shortFqn(psiClass : PsiClass) : SegmentedPath[List, String] =
  shortFqn(
    SegmentedPath
      .fromQualified(psiClass.getQualifiedName)
  )
end shortFqn

/**
 * Makes a shortened version of the FQN which accounts for the present imports
 */
def shortFqn(fqn : SegmentedPath[List, String]) : SegmentedPath[List, String] =
  imports.flatMap(nextImport =>
    if fqn.startsWith(nextImport) then
      Some(fqn.withoutCommonPartWith(nextImport))
    else
      None
  ).minByOption(_.length).getOrElse(fqn)
end shortFqn

/**
 * Makes possible real versions of shortened FQN
 *
 * Yes, it is a crunch, but we have only 4 imports, so it is okay.
 */
def makePossibleImports[F[_] : {Applicative, SemigroupK as SK}](name : SegmentedPath[F, String]) : List[SegmentedPath[F, String]] =
  given Semigroup[F[String]] = SK.algebra
  name :: imports.map(_.mapK(nonEmptyListToSemigroupK)).map(_ + name)
end makePossibleImports
