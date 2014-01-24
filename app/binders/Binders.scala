package binders

import play.api.mvc.QueryStringBindable.Parsing
import play.api.i18n.Lang
import play.api.mvc.QueryStringBindable
import java.util.Locale

/**
 * Created by jiemakel on 24.10.2013.
 */
object Binders {
  implicit object bindableLocale extends Parsing[Locale](
    new Locale(_), _.toString, (key: String, e: Exception) => "Cannot parse parameter %s as Locale: %s".format(key, e.getMessage)
  )
}
