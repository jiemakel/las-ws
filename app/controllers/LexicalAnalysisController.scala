/**
 *
 */
package controllers

import play.api.mvc._
import play.api.libs.json.{JsValue, Json}
import play.api.Routes
import fi.seco.lexical.ILexicalAnalysisService
import fi.seco.lexical.LanguageRecognizer
import fi.seco.lexical.CompoundLexicalAnalysisService
import fi.seco.lexical.hfst.HFSTLexicalAnalysisService
import fi.seco.lexical.SnowballLexicalAnalysisService
import scala.collection.convert.WrapAsScala._
import scala.collection.convert.WrapAsJava._
import java.util.Locale
import play.api.libs.json.Writes
import fi.seco.lexical.hfst.HFSTLexicalAnalysisService.WordToResults
import scala.Some
import scala.util.Try
import play.api.mvc.Result
import java.util
import services.lexicalanalysis.LanguageDetector
import play.api.libs.iteratee.{Iteratee, Concurrent}
import scala.concurrent.ExecutionContext.Implicits.global
import fi.seco.lexical.combined.CombinedLexicalAnalysisService
import java.util.Collections

/**
 * @author jiemakel
 *
 */
class LexicalAnalysisController(las: CompoundLexicalAnalysisService, hfstlas: CombinedLexicalAnalysisService, snowballlas: SnowballLexicalAnalysisService) extends Controller {

  def CORSAction(f: Request[AnyContent] => Result): Action[AnyContent] = {
    Action { request =>
      f(request).withHeaders("Access-Control-Allow-Origin" -> "*")
    }
  }

  def CORSAction[A](bp: BodyParser[A])(f: Request[A] => Result): Action[A] = {
    Action(bp) { request =>
      f(request).withHeaders("Access-Control-Allow-Origin" -> "*")
    }
  }

  def options = Action {
    Ok("").withHeaders("Access-Control-Allow-Origin" -> "*", "Access-Control-Allow-Methods" -> "POST, GET, OPTIONS, PUT, DELETE", "Access-Control-Max-Age" -> "3600", "Access-Control-Allow-Headers" -> "Origin, X-Requested-With, Content-Type, Accept", "Access-Control-Allow-Credentials" -> "true")
  }

  def index = Action {
    Ok(views.html.index(this,LanguageRecognizer.getAvailableLanguages, LanguageDetector.supportedLanguages, snowballlas.getSupportedBaseformLocales.map(_.toString), hfstlas.getSupportedBaseformLocales.map(_.toString), hfstlas.getSupportedAnalyzeLocales.map(_.toString),hfstlas.getSupportedInflectionLocales.map(_.toString),hfstlas.getSupportedHyphenationLocales.map(_.toString) ))
  }

  implicit def toResponse(res : Either[(JsValue, String),Either[String,JsValue]])(implicit request : Request[AnyContent], pretty: Option[String]) : Result = {
    res match {
      case Left(x) =>
        if (Accepts.Html.unapply(request)) Redirect(x._2)
        else Ok(x._1)
      case Right(x) => x match {
        case Left(y) => NotImplemented(y)
        case Right(y) =>
          if (pretty.isDefined && (pretty.get=="" || pretty.get.toBoolean)) Ok(Json.prettyPrint(y))
          else Ok(y)
      }
    }
  }

  def getBestLang(text: String, locales: Seq[String]) : Option[String] = {
    if (locales.isEmpty) {
    val lrResult = Option(LanguageRecognizer.getLanguageAsObject(text)).filter(_.getLang()!=null).map(r => Map(r.getLang -> r.getIndex))
    val ldResult = Try(LanguageDetector(text).map(l => Map(l.getLocale.toString -> l.getProbability))).getOrElse(Seq.empty)
    val hfstResultTmp = hfstlas.getSupportedAnalyzeLocales.map(lang =>
      (lang.toString(),hfstlas.recognize(text, lang))).filter(_._2!=0.0).toSeq.sortBy(_._2).reverse.map(p => (p._1,p._2*p._2))
    val tc = hfstResultTmp.foldRight(0.0) {_._2+_}
    val hfstResult = hfstResultTmp.map(p => Map(p._1 -> p._2/tc))
    Try(Some((ldResult ++ hfstResult ++ lrResult).groupBy(_.keysIterator.next).mapValues(_.foldRight(0.0){(p,r) => r+p.valuesIterator.next}/3.0).maxBy(_._2)._1)).getOrElse(None)
    } else {
      val lrResult = Option(LanguageRecognizer.getLanguageAsObject(text,locales:_*)).map(r => Map(r.getLang() -> r.getIndex))
      val ldResult = Try(LanguageDetector(text).filter(d => locales.contains(d.getLocale.toString)).map(l => Map(l.getLocale.toString -> l.getProbability))).getOrElse(Seq.empty)
      val hfstResultTmp = locales.map(new Locale(_)).intersect(hfstlas.getSupportedAnalyzeLocales.toSeq).map(lang =>
      (lang.toString(),hfstlas.recognize(text, lang))).filter(_._2!=0.0).toSeq.sortBy(_._2).reverse.map(p => (p._1,p._2*p._2))
      val tc = hfstResultTmp.foldRight(0.0) {_._2+_}
      val hfstResult = hfstResultTmp.map(p => Map(p._1 -> p._2/tc))
      Try(Some((ldResult ++ hfstResult ++ lrResult).groupBy(_.keysIterator.next).mapValues(_.foldRight(0.0){(p,r) => r+p.valuesIterator.next}/3.0).maxBy(_._2)._1)).getOrElse(None)

    }
  }
  
  def identify(text: Option[String], locales: Seq[String]) : Either[(JsValue, String),Either[String,JsValue]] = {
    text match {
      case Some(text) =>
        if (!locales.isEmpty) {
          val lrResult = Option(LanguageRecognizer.getLanguageAsObject(text,locales:_*)).filter(_.getLang()!=null).map(r => Map(r.getLang() -> r.getIndex))
          val ldResult = Try(LanguageDetector(text).filter(d => locales.contains(d.getLocale.toString)).map(l => Map(l.getLocale.toString -> l.getProbability))).getOrElse(Seq.empty)
          val hfstResultTmp = locales.map(new Locale(_)).intersect(hfstlas.getSupportedAnalyzeLocales.toSeq).map(lang =>
            (lang.toString(),hfstlas.recognize(text, lang))).filter(_._2!=0.0).toSeq.sortBy(_._2).reverse.map(p => (p._1,p._2*p._2))
          val tc = hfstResultTmp.foldRight(0.0) {_._2+_}
          val hfstResult = hfstResultTmp.map(p => Map(p._1 -> p._2/tc))
          val bestGuess = Try(Some((ldResult ++ hfstResult ++ lrResult).groupBy(_.keysIterator.next).mapValues(_.foldRight(0.0){(p,r) => r+p.valuesIterator.next}/3.0).maxBy(_._2))).getOrElse(None)
          bestGuess match {
            case Some(lang) => Right(Right(Json.toJson(Map("locale"->Json.toJson(lang._1),"certainty" -> Json.toJson(lang._2),"details"->Json.toJson(Map("languageRecognizerResults"->Json.toJson(lrResult), "languageDetectorResults" -> Json.toJson(ldResult), "hfstAcceptorResults" -> Json.toJson(hfstResult)))))))
            case None       => Right(Left(s"Couldn't categorize $text into any of requested languages (${locales.mkString(", ")})"))
          }
        } else {
          val lrResult = Option(LanguageRecognizer.getLanguageAsObject(text)).map(r => Map(r.getLang() -> r.getIndex))
          val ldResult = Try(LanguageDetector(text).map(l => Map(l.getLocale.toString -> l.getProbability))).getOrElse(Seq.empty)
          val hfstResultTmp = hfstlas.getSupportedAnalyzeLocales.map(lang =>
            (lang.toString(),hfstlas.recognize(text, lang))).filter(_._2!=0.0).toSeq.sortBy(_._2).reverse.map(p => (p._1,p._2*p._2))
          val tc = hfstResultTmp.foldRight(0.0) {_._2+_}
          val hfstResult = hfstResultTmp.map(p => Map(p._1 -> p._2/tc))
          val bestGuess = Try(Some((ldResult ++ hfstResult ++ lrResult).groupBy(_.keysIterator.next).mapValues(_.foldRight(0.0){(p,r) => r+p.valuesIterator.next}/3.0).maxBy(_._2))).getOrElse(None)
          bestGuess match {
            case Some(lang) => Right(Right(Json.toJson(Map("locale"->Json.toJson(lang._1),"certainty" -> Json.toJson(lang._2),"details"->Json.toJson(Map("languageRecognizerResults"->Json.toJson(lrResult), "languageDetectorResults" -> Json.toJson(ldResult), "hfstAcceptorResults" -> Json.toJson(hfstResult)))))))
            case None       => Right(Left(s"Couldn't categorize $text into any of the supported languages (${(LanguageRecognizer.getAvailableLanguages ++ LanguageDetector.supportedLanguages ++ hfstlas.getSupportedAnalyzeLocales.map(_.toString)).sorted.distinct.mkString(", ")})"))
          }
        }
      case None =>
        Left((Json.toJson(Map( "acceptedLocales" -> (LanguageRecognizer.getAvailableLanguages ++ LanguageDetector.supportedLanguages ++ hfstlas.getSupportedAnalyzeLocales.map(_.toString)).sorted.distinct)),controllers.routes.LexicalAnalysisController.index + "#language_recognition"))
    }
  }

  def toWSResponse(res : Either[(JsValue, String),Either[String,JsValue]]) : JsValue = {
    res match {
      case Left(x) => x._1
      case Right(x) => x match {
        case Left(y) => Json.toJson(Map("error" -> y))
        case Right(y) => y
      }
    }
  }

  def identifyGET(text: Option[String], locales: List[String],pretty:Option[String]) = CORSAction { implicit request =>
    implicit val ipretty = pretty;
    identify(text,locales)
  }

  def identifyPOST = CORSAction { implicit request =>
    val formBody = request.body.asFormUrlEncoded;
    val jsonBody = request.body.asJson;
    formBody.map { data =>
      implicit val ipretty = data.get("pretty").map(_.head)
      toResponse(identify(data.get("text").map(_.head),data.get("locales").getOrElse(Seq.empty)))
    }.getOrElse {
      jsonBody.map { data =>
        implicit val ipretty = (data \ "pretty").asOpt[String]
        toResponse(identify((data \ "text").asOpt[String],(data \ "locales").asOpt[Seq[String]].getOrElse(Seq.empty)))
      }.getOrElse {
        BadRequest("Expecting either a JSON or a form-url-encoded body")
      }
    }
  }

  def identifyWS = WebSocket.using[JsValue] { req =>

    //Concurrent.broadcast returns (Enumerator, Concurrent.Channel)
    val (out,channel) = Concurrent.broadcast[JsValue]

    //log the message to stdout and send response back to client
    val in = Iteratee.foreach[JsValue] {
      data => channel push toWSResponse(identify((data \ "text").asOpt[String],(data \ "locales").asOpt[Seq[String]].getOrElse(Seq.empty)))
    }
    (in,out)
  }

  def baseform(text: Option[String], locale: Option[Locale], segment:Boolean, guess:Boolean,depth:Int) : Either[(JsValue,String),Either[String,JsValue]] = {
    text match {
      case Some(text) =>
        locale match {
          case Some(locale) =>
            if (hfstlas.getSupportedBaseformLocales.contains(locale)) Right(Right(Json.toJson(hfstlas.baseform(text, locale,segment,guess,depth)))) else
            if (las.getSupportedBaseformLocales.contains(locale)) Right(Right(Json.toJson(las.baseform(text, locale,segment,guess)))) else Right(Left(s"Locale $locale not in the supported locales (${las.getSupportedBaseformLocales.mkString(", ")})"))
          case None => getBestLang(text,las.getSupportedBaseformLocales.toSeq.map(_.toString)) match {
            case Some(lang) =>
              if (hfstlas.getSupportedBaseformLocales.contains(locale)) Right(Right(Json.toJson(Map("locale" -> lang, "baseform" -> hfstlas.baseform(text, new Locale(lang),segment,guess,depth)))))
              else Right(Right(Json.toJson(Map("locale" -> lang, "baseform" -> las.baseform(text, new Locale(lang),segment,guess)))))
            case None       => Right(Left(s"Couldn't categorize $text into any of the supported languages (${las.getSupportedBaseformLocales.mkString(", ")})"))
          }
        }
      case None =>
        Left((Json.toJson(Map( "acceptedLocales" -> las.getSupportedBaseformLocales.map(_.toString).toSeq.sorted)),controllers.routes.LexicalAnalysisController.index + "#lemmatization"))
    }
  }

  def baseformGET(text: Option[String], locale: Option[Locale], segment: Boolean, guess: Boolean, depth:Int,pretty:Option[String]) = CORSAction { implicit request =>
    implicit val ipretty = pretty;
    baseform(text,locale,segment,guess,depth)
  }

  def baseformPOST = CORSAction { implicit request =>
    val formBody = request.body.asFormUrlEncoded;
    val jsonBody = request.body.asJson;
    formBody.map { data =>
      implicit val ipretty = data.get("pretty").map(_.head)
      toResponse(baseform(data.get("text").map(_.head),data.get("locale").map(l => new Locale(l.head)),data.get("segment").map(s => Try(s.head.toBoolean).getOrElse(false)).getOrElse(false),data.get("guess").map(s => Try(s.head.toBoolean).getOrElse(true)).getOrElse(true),data.get("depth").map(_.head.toInt).getOrElse(2)))
    }.getOrElse {
      jsonBody.map { data =>
        implicit val ipretty = (data \ "pretty").asOpt[String]
        toResponse(baseform((data \ "text").asOpt[String],(data \ "locale").asOpt[String].map(l => new Locale(l)),(data \ "segment").asOpt[Boolean].getOrElse(false),(data \ "guess").asOpt[Boolean].getOrElse(true),(data \ "depth").asOpt[Int].getOrElse(2)))
      }.getOrElse {
        BadRequest("Expecting either a JSON or a form-url-encoded body")
      }
    }
  }

  def baseformWS = WebSocket.using[JsValue] { req =>

    //Concurrent.broadcast returns (Enumerator, Concurrent.Channel)
    val (out,channel) = Concurrent.broadcast[JsValue]

    //log the message to stdout and send response back to client
    val in = Iteratee.foreach[JsValue] {
      data => channel push toWSResponse(baseform((data \ "text").asOpt[String],(data \ "locale").asOpt[String].map(l => new Locale(l)),(data \ "segment").asOpt[Boolean].getOrElse(false),(data \ "guess").asOpt[Boolean].getOrElse(true),(data \ "depth").asOpt[Int].getOrElse(2)))
    }
    (in,out)
  }

  implicit val WordPartWrites = new Writes[HFSTLexicalAnalysisService.Result.WordPart] {
    def writes(r : HFSTLexicalAnalysisService.Result.WordPart) : JsValue = {
      Json.obj(
        "lemma" -> r.getLemma,
        "tags" -> Json.toJson(r.getTags.toMap.mapValues(iterableAsScalaIterable(_)))
      )
    }
  }

  implicit val ResultWrites = new Writes[HFSTLexicalAnalysisService.Result] {
    def writes(r : HFSTLexicalAnalysisService.Result) : JsValue = {
      Json.obj(
        "weight" -> r.getWeight,
        "wordParts" -> Json.toJson(r.getParts.map(Json.toJson(_))),
        "globalTags" -> Json.toJson(r.getGlobalTags.toMap.mapValues(iterableAsScalaIterable(_)))
      )
    }
  }

  implicit val wordToResultsWrites = new Writes[WordToResults] {
    def writes(r: WordToResults) : JsValue = {
      Json.obj(
         "word" -> r.getWord,
         "analysis" -> Json.toJson(r.getAnalysis.map(Json.toJson(_)))
      )
    }
  }

  def analyze(text: Option[String], locale: Option[Locale], forms: Seq[String], segment:Boolean, guess:Boolean, segmentGuessed:Boolean, depth: Int) : Either[(JsValue,String),Either[String,JsValue]] = {
    text match {
      case Some(text) =>
        locale match {
          case Some(locale) => if (hfstlas.getSupportedAnalyzeLocales.contains(locale)) Right(Right(Json.toJson(hfstlas.analyze(text, locale, forms,segment,guess,segmentGuessed,depth).toList))) else Right(Left(s"Locale $locale not in the supported locales (${hfstlas.getSupportedAnalyzeLocales.mkString(", ")})"))
          case None => getBestLang(text,hfstlas.getSupportedAnalyzeLocales.toSeq.map(_.toString)) match {
            case Some(lang) => Right(Right(Json.toJson(Map("locale" -> Json.toJson(lang), "analysis" -> Json.toJson(hfstlas.analyze(text, new Locale(lang), forms, segment, guess, segmentGuessed, depth).toList)))))
            case None       => Right(Left(s"Couldn't categorize $text into any of the supported languages (${hfstlas.getSupportedAnalyzeLocales.mkString(", ")})"))
          }
        }
      case None =>
        Left((Json.toJson(Map( "acceptedLocales" -> hfstlas.getSupportedAnalyzeLocales.map(_.toString).toSeq.sorted))),controllers.routes.LexicalAnalysisController.index + "#morphological_analysis")
    }
  }

  def analyzeGET(text: Option[String], locale: Option[Locale], forms: Seq[String], segment:Boolean, guess:Boolean, segmentGuessed:Boolean,depth:Int,pretty:Option[String]) = CORSAction { implicit request =>
    implicit val ipretty = pretty;
    analyze(text,locale, forms,segment,guess,segmentGuessed,depth)
  }

  def analyzePOST = CORSAction { implicit request =>
    val formBody = request.body.asFormUrlEncoded;
    val jsonBody = request.body.asJson;
    formBody.map { data =>
      implicit val ipretty = data.get("pretty").map(_.head)
      toResponse(analyze(data.get("text").map(_.head),data.get("locale").map(l => new Locale(l.head)),data.get("forms").getOrElse(Seq.empty),data.get("segment").map(s => Try(s.head.toBoolean).getOrElse(false)).getOrElse(false),data.get("guess").map(s => Try(s.head.toBoolean).getOrElse(true)).getOrElse(true),data.get("segmentGuessed").map(s => Try(s.head.toBoolean).getOrElse(false)).getOrElse(false),data.get("depth").map(_.head.toInt).getOrElse(2)))
    }.getOrElse {
      jsonBody.map { data =>
        implicit val ipretty = (data \ "pretty").asOpt[String]
        toResponse(analyze((data \ "text").asOpt[String],(data \ "locale").asOpt[String].map(l => new Locale(l)),(data \ "forms").asOpt[Seq[String]].getOrElse(Seq.empty),(data \ "segment").asOpt[Boolean].getOrElse(false),(data \ "guess").asOpt[Boolean].getOrElse(true),(data \ "segmentGuessed").asOpt[Boolean].getOrElse(false),(data \ "depth").asOpt[Int].getOrElse(2)))
      }.getOrElse {
        BadRequest("Expecting either a JSON or a form-url-encoded body")
      }
    }
  }

  def analyzeWS = WebSocket.using[JsValue] { req =>

    //Concurrent.broadcast returns (Enumerator, Concurrent.Channel)
    val (out,channel) = Concurrent.broadcast[JsValue]

    //log the message to stdout and send response back to client
    val in = Iteratee.foreach[JsValue] {
      data => channel push toWSResponse(analyze((data \ "text").asOpt[String],(data \ "locale").asOpt[String].map(l => new Locale(l)),(data \ "forms").asOpt[Seq[String]].getOrElse(Seq.empty),(data \ "segment").asOpt[Boolean].getOrElse(false),(data \ "guess").asOpt[Boolean].getOrElse(true),(data \ "segmentGuessed").asOpt[Boolean].getOrElse(false),(data \ "depth").asOpt[Int].getOrElse(2)))
    }
    (in,out)
  }

  def inflect(text: Option[String], forms: Seq[String], segment: Boolean, baseform: Boolean, guess: Boolean, locale : Option[Locale]) : Either[(JsValue, String),Either[String,JsValue]] = {
    text match {
      case Some(text) =>
        locale match {
          case Some(locale) => if (hfstlas.getSupportedInflectionLocales.contains(locale)) Right(Right(Json.toJson(hfstlas.inflect(text, forms, segment, baseform, guess, locale)))) else Right(Left(s"Locale $locale not in the supported locales (${hfstlas.getSupportedInflectionLocales.mkString(", ")})"))
          case None => getBestLang(text,hfstlas.getSupportedInflectionLocales.toSeq.map(_.toString)) match {
            case Some(lang) => Right(Right(Json.toJson(Map("locale" -> Json.toJson(lang), "inflection" -> Json.toJson(hfstlas.inflect(text, forms, segment, baseform, guess, new Locale(lang)))))))
            case None       => Right(Left(s"Couldn't categorize $text into any of the supported languages (${hfstlas.getSupportedInflectionLocales.mkString(", ")})"))
          }
        }
      case None =>
        Left((Json.toJson(Map( "acceptedLocales" -> hfstlas.getSupportedInflectionLocales.map(_.toString).toSeq.sorted))),controllers.routes.LexicalAnalysisController.index + "#morphological_analysis")
    }
  }


  def inflectGET(text: Option[String], forms: Seq[String], segment: Boolean, baseform: Boolean, guess: Boolean, locale : Option[Locale],pretty:Option[String]) = CORSAction { implicit request =>
    implicit val ipretty = pretty;
    inflect(text,forms,segment,baseform,guess,locale)
  }

  def inflectPOST = CORSAction { implicit request =>
    val formBody = request.body.asFormUrlEncoded;
    val jsonBody = request.body.asJson;
    formBody.map { data =>
      implicit val ipretty = data.get("pretty").map(_.head)
      toResponse(inflect(data.get("text").map(_.head),data.get("forms").getOrElse(Seq.empty),data.get("segment").map(s => Try(s.head.toBoolean).getOrElse(false)).getOrElse(false),data.get("baseform").map(s => Try(s.head.toBoolean).getOrElse(true)).getOrElse(true),data.get("guess").map(s => Try(s.head.toBoolean).getOrElse(true)).getOrElse(true),data.get("locale").map(l => new Locale(l.head))))
    }.getOrElse {
      jsonBody.map { data =>
        implicit val ipretty = (data \ "pretty").asOpt[String]
        toResponse(inflect((data \ "text").asOpt[String],(data \ "forms").asOpt[Seq[String]].getOrElse(Seq.empty), (data \ "segment").asOpt[Boolean].getOrElse(false), (data \ "baseform").asOpt[Boolean].getOrElse(true), (data \ "guess").asOpt[Boolean].getOrElse(true), (data \ "locale").asOpt[String].map(l => new Locale(l))))
      }.getOrElse {
        BadRequest("Expecting either a JSON or a form-url-encoded body")
      }
    }
  }

  def inflectWS = WebSocket.using[JsValue] { req =>

    //Concurrent.broadcast returns (Enumerator, Concurrent.Channel)
    val (out,channel) = Concurrent.broadcast[JsValue]

    //log the message to stdout and send response back to client
    val in = Iteratee.foreach[JsValue] {
      data => channel push toWSResponse(inflect((data \ "text").asOpt[String],(data \ "forms").asOpt[Seq[String]].getOrElse(Seq.empty), (data \ "segment").asOpt[Boolean].getOrElse(false), (data \ "baseform").asOpt[Boolean].getOrElse(true), (data \ "guess").asOpt[Boolean].getOrElse(true), (data \ "locale").asOpt[String].map(l => new Locale(l))))
    }
    (in,out)
  }

  def hyphenate(text: Option[String], locale: Option[Locale]) : Either[(JsValue,String),Either[String,JsValue]] = {
    text match {
      case Some(text) =>
        locale match {
          case Some(locale) => if (hfstlas.getSupportedHyphenationLocales.contains(locale)) Right(Right(Json.toJson(hfstlas.hyphenate(text, locale)))) else Right(Left(s"Locale $locale not in the supported locales (${hfstlas.getSupportedHyphenationLocales.mkString(", ")})"))
          case None => getBestLang(text,hfstlas.getSupportedHyphenationLocales.toSeq.map(_.toString))  match {
            case Some(lang) => Right(Right(Json.toJson(Map("locale" -> Json.toJson(lang), "hyphenation" -> Json.toJson(hfstlas.hyphenate(text, new Locale(lang)))))))
            case None       => Right(Left(s"Couldn't categorize $text into any of the supported languages (${hfstlas.getSupportedHyphenationLocales.mkString(", ")})"))
          }
        }
      case None =>
        Left((Json.toJson(Map( "acceptedLocales" -> hfstlas.getSupportedHyphenationLocales.map(_.toString).toSeq.sorted))),controllers.routes.LexicalAnalysisController.index + "#hyphenation")
    }
  }

  def hyphenateGET(text: Option[String], locale: Option[Locale],pretty:Option[String]) = CORSAction { implicit request =>
    implicit val ipretty = pretty;
    hyphenate(text,locale)
  }

  def hyphenatePOST = CORSAction { implicit request =>
    val formBody = request.body.asFormUrlEncoded;
    val jsonBody = request.body.asJson;
    formBody.map { data =>
      implicit val ipretty = data.get("pretty").map(_.head)
      toResponse(hyphenate(data.get("text").map(_.head),data.get("locale").map(l => new Locale(l.head))))
    }.getOrElse {
      jsonBody.map { data =>
        implicit val ipretty = (data \ "pretty").asOpt[String]
        toResponse(hyphenate((data \ "text").asOpt[String],(data \ "locale").asOpt[String].map(l => new Locale(l))))
      }.getOrElse {
        BadRequest("Expecting either a JSON or a form-url-encoded body")
      }
    }
  }

  def hyphenateWS = WebSocket.using[JsValue] { req =>

    //Concurrent.broadcast returns (Enumerator, Concurrent.Channel)
    val (out,channel) = Concurrent.broadcast[JsValue]

    //log the message to stdout and send response back to client
    val in = Iteratee.foreach[JsValue] {
      data => channel push toWSResponse(hyphenate((data \ "text").asOpt[String],(data \ "locale").asOpt[String].map(l => new Locale(l))))
    }
    (in,out)
  }

  def javascriptRoutes = Action { implicit request =>
    Ok(Routes.javascriptRouter("jsRoutes")(routes.javascript.LexicalAnalysisController.baseformGET, routes.javascript.LexicalAnalysisController.analyzeGET, routes.javascript.LexicalAnalysisController.identifyGET, routes.javascript.LexicalAnalysisController.hyphenateGET,routes.javascript.LexicalAnalysisController.inflectGET)).as(JAVASCRIPT)
  }
}
