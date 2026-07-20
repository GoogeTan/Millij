package katze.millij.annotator.annotators

import cats.syntax.all.*
import com.intellij.openapi.util.text.HtmlChunk
import katze.millij
import katze.millij.data.Smart
import katze.millij.{MillijBundle, place}
import katze.millij.place.PlaceInYamlConfig.{Member, Module}
import katze.millij.place.{PlaceInYamlConfig, richPlaceOf, yamlDefinableMembersOfScope}
import katze.millij.scalatypes.{ScMapType, isMvnDependency}
import org.jetbrains.plugins.scala.lang.psi.types.{ScType, ScTypeExt, TypePresentationContext}
import org.jetbrains.plugins.scala.project.ProjectContext
import org.jetbrains.yaml.psi.{YAMLKeyValue, YAMLMapping}

def unexistingMembersError(mapping : YAMLMapping, kv : YAMLKeyValue)(using Smart) : Option[String] =
  given ProjectContext = ProjectContext.fromPsi(mapping)
  richPlaceOf(mapping).toOption.flatMap(scope =>
    scope match
      case Member(_, expectedType, _) if isMvnDependency(expectedType) =>
        None
      case Member(_, ScMapType(_, _), _) =>
        None
      case _ =>
        val possibleMembers = yamlDefinableMembersOfScope(scope)
        if possibleMembers.exists(_.name === kv.getKeyText) then
          None
        else
          given context: TypePresentationContext = TypePresentationContext(kv)
    
          val scopeText = scope match
            case Module(extendList, _, _) =>
              MillijBundle.message("module.extends", extendList.map(typeReferenceHtml).mkString(", "))
            case Member(_, expectedType, _) =>
              MillijBundle.message("object.of.type", s"${typeReferenceHtml(expectedType)}")
    
          Some(
            MillijBundle.message("unexisting.member.error", kv.getKeyText, scopeText)
          )
  )
end unexistingMembersError

//TODO finish working HTML links
def typeReferenceHtml(scType : ScType)(using TypePresentationContext) : String =
  val cleanFqn = scType.extractClass match
    case Some(psiClass) => psiClass.getQualifiedName
    case None => scType.canonicalText.takeWhile(_ != '[')
  HtmlChunk.link(s"#goto/$cleanFqn", scType.presentableText).toString
end typeReferenceHtml