package katze.millij.data

import cats.arrow.FunctionK
import cats.data.NonEmptyList
import cats.syntax.all.*
import cats.{Applicative, Foldable, Semigroup, SemigroupK, ~>}

def foldableToListK[F[_] : Foldable] : F ~> List =
  FunctionK.lift(
    [T] => (nel : F[T]) => nel.toList
  )
end foldableToListK

def nonEmptyListToSemigroupK[F[_]  : {Applicative as A, SemigroupK as SK}] : NonEmptyList ~> F =
  FunctionK.lift(
    [T] => lst =>
      given Semigroup[F[T]] = SK.algebra
      lst.map(A.pure).reduce
  )
end nonEmptyListToSemigroupK