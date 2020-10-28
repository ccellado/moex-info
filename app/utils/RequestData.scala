package utils
import play.api.libs.ws._
import scala.concurrent.{Future, Await, ExecutionContext}
import scala.concurrent.duration._
import play.api.mvc._
import javax.inject.{Inject, Singleton}
import scala.xml.NodeSeq

@Singleton
class RequestData @Inject() (ws: WSClient)(implicit ec: ExecutionContext) {
    val url = "http://iss.moex.com/iss/securities"

    def showSomeSiteContent(query: String): Future[NodeSeq] = {
      val req = ws
        .url(url)
        .addHttpHeaders("Accept" -> "application/xml")
        .addQueryStringParameters("q" -> query)
        .withRequestTimeout(10000.millis)
      req.get().map { responce => 
        responce.xml
      }
    }
  }

