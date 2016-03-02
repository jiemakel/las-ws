/**
 *
 */
package services.lexicalanalysis

import com.softwaremill.macwire.MacwireMacros._
import scala.collection.convert.WrapAsScala._
import scala.collection.convert.WrapAsJava._

import fi.seco.lexical.CompoundLexicalAnalysisService
import fi.seco.lexical.LanguageRecognizer
import fi.seco.lexical.SnowballLexicalAnalysisService
import com.typesafe.scalalogging.LazyLogging
import fi.seco.lexical.combined.CombinedLexicalAnalysisService
import com.optimaize.langdetect.profiles.LanguageProfileReader
import com.optimaize.langdetect.LanguageDetectorBuilder
import com.optimaize.langdetect.text.CommonTextObjectFactories
import com.optimaize.langdetect.ngram.NgramExtractors

/**
 * @author jiemakel
 *
 */
trait LexicalAnalysisModule {
  lazy val hfstlas = new CombinedLexicalAnalysisService()
  lazy val snowballlas = new SnowballLexicalAnalysisService()
  lazy val clas = new CompoundLexicalAnalysisService(hfstlas, snowballlas)

}

object LanguageDetector extends LazyLogging {
  val languageProfiles = new LanguageProfileReader().readAllBuiltIn()
  val supportedLanguages = languageProfiles.map(_.getLocale.toString())
  val detector = LanguageDetectorBuilder.create(NgramExtractors.standard()).withProfiles(languageProfiles).build()
  val textObjectFactory = CommonTextObjectFactories.forDetectingOnLargeText()
  def apply(text: String) = detector.getProbabilities(textObjectFactory.forText(text))
}