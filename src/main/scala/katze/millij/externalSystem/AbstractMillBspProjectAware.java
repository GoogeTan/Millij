package katze.millij.externalSystem;

import com.intellij.openapi.externalSystem.autoimport.ExternalSystemProjectAware;

/**
 * A Java bridge to prevent the Scala 3 compiler from generating broken
 * bytecode for Kotlin default methods that return inline classes.
 */
public abstract class AbstractMillBspProjectAware implements ExternalSystemProjectAware {
}