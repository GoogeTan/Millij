package katze.millij.place

import cats.Applicative
import cats.syntax.all.*
import katze.millij.file.elementsFilePath
import katze.millij.data.module.NamespacedPath
import katze.millij.data.{ScalaIdentifier, SegmentedPath}
import org.jetbrains.yaml.psi.{YAMLMapping, YAMLPsiElement}

final class EnclosingModulePathResolver[F[_] : Applicative](
  filePath : SegmentedPath[List, ScalaIdentifier],
  onUnexpectedElement : List[YAMLPsiElement] => F[NamespacedPath[List, ScalaIdentifier]]
) extends YAMLConfigResolver[F, NamespacedPath[List, ScalaIdentifier]]:
  override def field(
    parent: NamespacedPath[List, ScalaIdentifier],
    name: String
  ): F[NamespacedPath[List, ScalaIdentifier]] =
    parent.pure
  end field

  override def module(
    parent: NamespacedPath[List, ScalaIdentifier],
    name: ScalaIdentifier,
    mapping: YAMLMapping
  ): F[NamespacedPath[List, ScalaIdentifier]] =
    parent.addPathSegment(name).pure
  end module

  override def sequenceItem(
    parent: NamespacedPath[List, ScalaIdentifier]
  ): F[NamespacedPath[List, ScalaIdentifier]] =
    parent.pure

  override def topLevelModule(mapping: YAMLMapping): F[NamespacedPath[List, ScalaIdentifier]] =
    NamespacedPath(filePath, SegmentedPath(Nil)).pure
  end topLevelModule

  override def mapping(parent: NamespacedPath[List, ScalaIdentifier], mapping: YAMLMapping): F[NamespacedPath[List, ScalaIdentifier]] =
    parent.pure
  end mapping

  override def onUnexpected(element: List[YAMLPsiElement]): F[NamespacedPath[List, ScalaIdentifier]] =
    onUnexpectedElement(element)
  end onUnexpected
end EnclosingModulePathResolver

def enclosingModule(
  element: YAMLPsiElement
) : Option[NamespacedPath[List, ScalaIdentifier]] =
  elementsFilePath(element).toOption.flatMap(filePath =>
    enclosingModule(filePath, element)
  )
end enclosingModule

def enclosingModule(
  filePath : SegmentedPath[List, ScalaIdentifier],
  element: YAMLPsiElement
) : Option[NamespacedPath[List, ScalaIdentifier]] =
  placeOf(element, EnclosingModulePathResolver(filePath, _ => None))
end enclosingModule