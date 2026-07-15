package katze.millij.place

import katze.millij.data.ScalaIdentifier
import org.jetbrains.yaml.psi.*

trait YAMLConfigResolver[F[_], Place]:
  def field(parent: Place, name: String): F[Place]
  def module(parent: Place, name: ScalaIdentifier, mapping: YAMLMapping): F[Place]
  def sequenceItem(parent: Place): F[Place]
  def topLevelModule(mapping: YAMLMapping): F[Place]
  def mapping(parent: Place, mapping: YAMLMapping): F[Place]
  def onUnexpected(element: List[YAMLPsiElement]): F[Place]
end YAMLConfigResolver