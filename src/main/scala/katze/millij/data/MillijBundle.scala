package katze.millij.data

import com.intellij.DynamicBundle
import org.jetbrains.annotations.{Nls, NonNls, NotNull, PropertyKey}

import java.util.function.Supplier

object MillijBundle:
  @NonNls
  inline val BUNDLE = "messages.MillijBundle"
  
  private val INSTANCE = new DynamicBundle(classOf[MillijBundle.type], BUNDLE)
  
  
  @NotNull
  @Nls 
  def message(@NotNull @PropertyKey(resourceBundle = BUNDLE) key: String, params: AnyRef*): String =
    INSTANCE.getMessage(key, params*)
  end message
  
  def lazyMessage(@NotNull @PropertyKey(resourceBundle = BUNDLE) key: String, params: AnyRef*): Supplier[String] = 
    INSTANCE.getLazyMessage(key, params*)
  end lazyMessage
end MillijBundle