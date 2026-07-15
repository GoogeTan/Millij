package katze.millij.scalatypes

import cats.*
import cats.arrow.FunctionK
import cats.data.*
import cats.syntax.all.*
import com.intellij.psi.PsiClass
import katze.millij.data.{ScalaIdentifier, SegmentedPath, nonEmptyListToSemigroupK}

/**
 * Those imports are added by mill on top of every YAML config implicitly.
 */
val imports : List[SegmentedPath[NonEmptyList, ScalaIdentifier]] =
  List(
    "mill", "mill.scalalib", "mill.javalib", "mill.kotlinlib"
  )
    .map(SegmentedPath.fromQualifiedNonEmpty(_).get)
    .map(_.map(ScalaIdentifier.unsafe))

/**
 * Makes a shortened version of the FQN which accounts for the present imports
 */
def shortFqn(psiClass : PsiClass) : SegmentedPath[List, ScalaIdentifier] =
  shortFqn(
    SegmentedPath
      .fromQualified(psiClass.getQualifiedName)
      .map(ScalaIdentifier.unsafe)
  )
end shortFqn

/**
 * Makes a shortened version of the FQN which accounts for the present imports
 */
def shortFqn(fqn : SegmentedPath[List, ScalaIdentifier]) : SegmentedPath[List, ScalaIdentifier] =
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
def makePossibleImports[F[_] : {Applicative, SemigroupK as SK}](name : SegmentedPath[F, ScalaIdentifier]) : List[SegmentedPath[F, ScalaIdentifier]] =
  given Semigroup[F[ScalaIdentifier]] = SK.algebra
  name :: imports.map(_.mapK(nonEmptyListToSemigroupK)).map(_ + name)
end makePossibleImports
