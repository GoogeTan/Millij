package katze.millij.configuration

import com.intellij.execution.configurations.{ConfigurationFactory, ConfigurationType, RunConfiguration}
import com.intellij.openapi.project.Project

class MillRunConfigurationFactory(configType: ConfigurationType) extends ConfigurationFactory(configType):
  override def createTemplateConfiguration(project: Project): RunConfiguration =
    new MillRunConfiguration(project, this, "Mill")

  override def getId: String = "MillRunConfigurationFactory"
end MillRunConfigurationFactory
