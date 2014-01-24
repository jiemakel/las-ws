/**
 *
 */
package services.lexicalanalysis

import com.softwaremill.macwire.MacwireMacros._
import fi.seco.lexical.CompoundLexicalAnalysisService
import fi.seco.lexical.LanguageRecognizer
import fi.seco.lexical.hfst.HFSTLexicalAnalysisService
import fi.seco.lexical.SnowballLexicalAnalysisService
import com.cybozu.labs.langdetect.Detector
import scala.util.Try
import com.typesafe.scalalogging.slf4j.Logging

/**
 * @author jiemakel
 *
 */
trait LexicalAnalysisModule {
  lazy val hfstlas = new HFSTLexicalAnalysisService()
  lazy val snowballlas = new SnowballLexicalAnalysisService()
  lazy val clas = new CompoundLexicalAnalysisService(hfstlas, snowballlas)

}

object LanguageDetector extends Logging {
  def apply() = com.cybozu.labs.langdetect.DetectorFactory.create()
  val supportedLanguages = Array("af","am","ar","az","be","bg","bn","bo","ca","cs","cy","da","de","dv","el","en","es","et","eu","fa","fi","fo","fr","ga","gn","gu","he","hi","hr","hu","hy","id","is","it","ja","jv","ka","kk","km","kn","ko","ky","lb","lij","ln","lt","lv","mi","mk","ml", "mn", "mr", "mt", "my", "ne", "nl", "no", "os", "pa", "pl", "pnb", "pt", "qu", "ro", "si", "sk", "so", "sq", "sr", "sv", "sw", "ta", "te", "th", "tk", "tl", "tr", "tt", "ug", "uk", "ur", "uz", "vi", "yi", "yo", "zh-cn", "zh-tw")
  try {
    com.cybozu.labs.langdetect.DetectorFactory.loadProfiles(supportedLanguages:_*)
  } catch {
    case e: Exception => logger.warn("Couldn't load language profiles",e)
  }
}