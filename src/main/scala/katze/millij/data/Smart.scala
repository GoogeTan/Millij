package katze.millij.data

import com.intellij.openapi.project.{DumbService, Project}

import scala.annotation.implicitNotFound

/**
 * Used to indicate that a method requires IntelliJ smart mode.
 * @todo make it erased
 */
@implicitNotFound("Couldn't prove that being ran in smart mode. Consider adding using param of Smart to mark your method ad requiring smart.")
final class Smart private ()

object Smart:
  def apply(project : Project)[T](f : Smart ?=> T) : Option[T] =
    if !DumbService.isDumb(project) then
      Some(f(using new Smart()))
    else  
      None
  end apply
end Smart