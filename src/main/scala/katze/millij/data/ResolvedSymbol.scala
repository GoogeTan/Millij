package katze.millij.data

import cats.syntax.all.*
import com.intellij.psi.{PsiClass, PsiElement, PsiPackage}
import org.jetbrains.plugins.scala.lang.psi.api.toplevel.typedef.{ScClass, ScObject, ScTrait}
import org.jetbrains.plugins.scala.lang.psi.types.ScType
import org.jetbrains.plugins.scala.lang.psi.types.api.designator.{ScDesignatorType, ScProjectionType}
import org.jetbrains.plugins.scala.project.ProjectContext
import org.jetbrains.yaml.psi.YAMLMapping


enum ResolvedSymbol[Segment](val element : PsiElement):
  case ScalaTrait[Segment](
    parentType : Option[ScType],
    override val element : ScTrait
  ) extends ResolvedSymbol[Segment](element)
  case ScalaObject[Segment](
    parentType : Option[ScType], 
    override val  element: ScObject
  ) extends ResolvedSymbol[Segment](element)
  case ScalaClass[Segment](
    parentType: Option[ScType], 
    override val  element: ScClass
  ) extends ResolvedSymbol[Segment](element)
  case ScalaPackage[Segment](override val element: PsiPackage) extends ResolvedSymbol[Segment](element)
  case YamlModule[Segment](
    modulePath: SegmentedPath[List, Segment], 
    moduleType: ModuleType[Segment], 
    override val element : YAMLMapping
  ) extends ResolvedSymbol[Segment](element)

  //TODO handle classes here
  def scalaTraitType(using ProjectContext) : Option[ScType] =
    this match
      case ResolvedSymbol.ScalaTrait(Some(parentType), element) => Some(ScProjectionType(parentType, element))
      case ResolvedSymbol.ScalaTrait(None, element) => Some(ScDesignatorType(element))
      case _ => None
    end match
  end scalaTraitType

  def scTypeForQualifiedSearch(using ProjectContext) : Option[ScType] =
    this match
      case ResolvedSymbol.ScalaTrait(parentType, element) =>
        element.baseCompanion.collect:
          case ob : ScObject =>
            parentType.fold(
              ScDesignatorType(ob)
            )(
              ScProjectionType(_, ob)
            )
      case ResolvedSymbol.ScalaObject(parentType, element) =>
        Some(
          parentType.fold(
            ScDesignatorType(element)
          )(
            ScProjectionType(_, element)
          )
        )
      case ResolvedSymbol.ScalaClass(parentType, element) =>
        element.baseCompanion.collect:
          case ob : ScObject =>
            parentType.fold(
              ScDesignatorType(ob)
            )(
              ScProjectionType(_, ob)
            )
      case ResolvedSymbol.ScalaPackage(element) =>
        None
      case ResolvedSymbol.YamlModule(modulePath, moduleType, element) =>
        None
end ResolvedSymbol

object ResolvedSymbol:
  def fromPsiElement[Segment](psiClass : PsiElement) : Option[ResolvedSymbol[Segment]] =
    psiClass match
      case clazz: ScClass =>
        ResolvedSymbol.ScalaClass(None, clazz).some
      case scTrait: ScTrait =>
        ResolvedSymbol.ScalaTrait(None, scTrait).some
      case scObject: ScObject =>
        ResolvedSymbol.ScalaObject(None, scObject).some
      case psiPackage: PsiPackage =>
        ResolvedSymbol.ScalaPackage(psiPackage).some
      case _ =>
        None
    end match
  end fromPsiElement
end ResolvedSymbol