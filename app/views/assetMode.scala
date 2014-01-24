package views.html.helper

import play.api.templates.Html
import play.api.mvc.{Call}
import play.api.{Play, Mode}
import controllers.routes

/** Make the app explicit for testing */
trait RequiresApp {
  implicit val app = play.api.Play.current
}

object assetMode extends RequiresApp {
  def apply(scriptNameDev: String)(scriptNameProd: String = scriptNameDev)(scriptNameTest : String = scriptNameProd): String = {
    app.mode match {
      case Mode.Dev => scriptNameDev
      case Mode.Test => scriptNameTest
      case Mode.Prod => scriptNameProd
    }
  }
}
