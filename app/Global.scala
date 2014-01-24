import com.softwaremill.macwire.{InstanceLookup, Macwire}
import java.util.Locale
import play.api.GlobalSettings
import play.api.mvc.QueryStringBindable.Parsing

object Global extends GlobalSettings with Macwire {
  val instanceLookup = InstanceLookup(valsByClass(Application))

  override def getControllerInstance[A](controllerClass: Class[A]) = instanceLookup.lookupSingleOrThrow(controllerClass)

}
