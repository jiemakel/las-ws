import controllers.LexicalAnalysisController

import com.softwaremill.macwire.MacwireMacros._
import services.lexicalanalysis.LexicalAnalysisModule

object Application extends LexicalAnalysisModule {

  val lexicalAnalysisController = wire[LexicalAnalysisController]

}
