package katze.millij.place

import cats.Applicative
import katze.millij.data.ScalaIdentifier
import org.jetbrains.yaml.psi.{YAMLMapping, YAMLPsiElement}

//TODO document me
/**
 * Calculated whether given element is in module scope or not(it is in some member then)
 * @param onUnexpectedError What to do on unexpected element
 * @tparam F An effect for soring and error after encounter with unexpected element
 */
final class ScopeTypeConfigResolver[F[_] : Applicative as FA](
  onUnexpectedError : List[YAMLPsiElement] => F[Boolean]
) extends YAMLConfigResolver[F, Boolean]:
  override def onUnexpected(element: List[YAMLPsiElement]): F[Boolean] =
    onUnexpectedError(element)
  end onUnexpected

  override def topLevelModule(mapping: YAMLMapping): F[Boolean] =
    FA.pure(true)
  end topLevelModule

  override def mapping(parent: Boolean, mapping: YAMLMapping): F[Boolean] =
    FA.pure(parent)
  end mapping

  override def field(parent: Boolean, name: String): F[Boolean] =
    FA.pure(false)
  end field

  override def sequenceItem(parent: Boolean): F[Boolean] =
    FA.pure(false)
  end sequenceItem

  override def module(parent: Boolean, name: ScalaIdentifier, mapping: YAMLMapping): F[Boolean] =
    FA.pure(true)
  end module
end ScopeTypeConfigResolver

