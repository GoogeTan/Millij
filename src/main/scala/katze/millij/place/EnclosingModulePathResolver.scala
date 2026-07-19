package katze.millij.place

import cats.Applicative
import cats.syntax.all.*
import katze.millij.file.elementsFilePath
import katze.millij.data.module.NamespacedPath
import katze.millij.data.SegmentedPath
import org.jetbrains.yaml.psi.{YAMLMapping, YAMLPsiElement}

final class EnclosingModulePathResolver[F[_] : Applicative](
  filePath : SegmentedPath[List, String],
  onUnexpectedElement : List[YAMLPsiElement] => F[NamespacedPath[List, String]]
) extends YAMLConfigResolver[F, NamespacedPath[List, String]]:
  override def field(
    parent: NamespacedPath[List, String],
    name: String
  ): F[NamespacedPath[List, String]] =
    parent.pure
  end field

  override def module(
    parent: NamespacedPath[List, String],
    name: String,
    mapping: YAMLMapping
  ): F[NamespacedPath[List, String]] =
    parent.addPathSegment(name).pure
  end module

  override def sequenceItem(
    parent: NamespacedPath[List, String]
  ): F[NamespacedPath[List, String]] =
    parent.pure

  override def topLevelModule(mapping: YAMLMapping): F[NamespacedPath[List, String]] =
    NamespacedPath(filePath, SegmentedPath(Nil)).pure
  end topLevelModule

  override def mapping(parent: NamespacedPath[List, String], mapping: YAMLMapping): F[NamespacedPath[List, String]] =
    parent.pure
  end mapping

  override def onUnexpected(element: List[YAMLPsiElement]): F[NamespacedPath[List, String]] =
    onUnexpectedElement(element)
  end onUnexpected
end EnclosingModulePathResolver

def enclosingModule(
  element: YAMLPsiElement
) : Option[NamespacedPath[List, String]] =
  elementsFilePath(element).toOption.flatMap(filePath =>
    enclosingModule(filePath, element)
  )
end enclosingModule

def enclosingModule(
  filePath : SegmentedPath[List, String],
  element: YAMLPsiElement
) : Option[NamespacedPath[List, String]] =
  placeOf(element, EnclosingModulePathResolver(filePath, _ => None))
end enclosingModule