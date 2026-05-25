package katze.millij.configuration

import com.intellij.execution.configurations.{ConfigurationFactory, ConfigurationType, ConfigurationTypeUtil}
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.IconLoader
import org.jetbrains.annotations.{Nls, NonNls}

import javax.swing.Icon


class MillRunConfigurationType extends ConfigurationType:
  lazy val icon = IconLoader.getIcon("/icons/millFile.svg", classOf[MillRunConfigurationType])
  
  override def getDisplayName: String = "Mill"

  override def getConfigurationTypeDescription: String = "Mill run configuration"

  override def getIcon: Icon = icon

  @NonNls
  override def getId: String = MillRunConfigurationType.Id

  override def getConfigurationFactories: Array[ConfigurationFactory] = 
    Array[ConfigurationFactory](new MillRunConfigurationFactory(this))
  end getConfigurationFactories
end MillRunConfigurationType

object MillRunConfigurationType:
  val Id: String = "MillRunConfiguration"
end MillRunConfigurationType
