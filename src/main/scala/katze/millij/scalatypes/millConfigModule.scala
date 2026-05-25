package katze.millij.scalatypes

import com.intellij.openapi.module.{Module, ModuleManager}
import com.intellij.openapi.project.Project

/**
 * Returns a module that contains mill config files.
 */
def millConfigModule(project: Project): Option[Module] = 
  val modules = ModuleManager.getInstance(project).getModules
  modules.find(_.getName.startsWith("mill-build"))
end millConfigModule
