package katze.millij

import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightProjectDescriptor

object MillProjectDescriptor extends Scala3ProjectDescriptor:
  override def setUpProject(project: Project, handler: LightProjectDescriptor.SetupHandler): Unit =
    super.setUpProject(project, handler)
    WriteAction.run: () =>
      val manager = ModuleManager.getInstance(project)

      manager.getModules.headOption.foreach: module =>
        if module.getName != "mill-build" then
          val model = manager.getModifiableModel
          model.renameModule(module, "mill-build")
          model.commit()
          