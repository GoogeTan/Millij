package katze.millij

import cats.syntax.all.*
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.{JavaSdk, Sdk}
import com.intellij.openapi.roots.{ContentEntry, LibraryOrderEntry, ModifiableRootModel}
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.testFramework.fixtures.{DefaultLightProjectDescriptor, MavenDependencyUtil}
import org.jetbrains.plugins.scala.project.ScalaLibraryType

class Scala3ProjectDescriptor extends DefaultLightProjectDescriptor:
  override def getSdk: Sdk =
    val home = System.getProperty("java.home")
    JavaSdk.getInstance().createJdk("java home", home)
  end getSdk
  
  override def configureModule(module: Module, model: ModifiableRootModel, contentEntry: ContentEntry): Unit =
    super.configureModule(module, model, contentEntry)

    MavenDependencyUtil.addFromMaven(model, "org.scala-lang:scala-library:2.13.12")
    MavenDependencyUtil.addFromMaven(model, "org.scala-lang:scala3-library_3:3.3.3")

    model.getOrderEntries.foreach:
      case entry: LibraryOrderEntry =>
        val isScala = Option(entry.getLibraryName).exists: name =>
          name.contains("scala3-library") || name.contains("scala-library")

        if isScala then
          entry.getLibrary match
            case libEx: LibraryEx =>
              val libModel = libEx.getModifiableModel
              libModel.setKind(ScalaLibraryType.Kind)
              libModel.commit()
            case _ =>
      case _ =>
  end configureModule
end Scala3ProjectDescriptor

object Scala3ProjectDescriptor extends Scala3ProjectDescriptor