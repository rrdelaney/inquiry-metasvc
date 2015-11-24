package actors.processors

import akka.actor._
import play.api.libs.ws._
import scala.concurrent.{Future, Await}
import scala.concurrent.duration._
import play.api.libs.json.JsValue
import dao.VideoDAO

import play.api.libs.concurrent.Execution.Implicits.defaultContext

class ClarifaiProcessingActor(ws: WSClient, videoDAO: VideoDAO) extends Actor {
  import actors.ImageProcessingActor._

  def receive = {
    case ClarifaiImage(id: String, frame: String, token: String) =>
      val url = s"https://api.clarifai.com/v1/tag/?url=http://192.241.191.143/frames/$id/$frame.png"
      val keywordsFuture: Future[Seq[JsValue]] = ws.url(url)
        .withHeaders("Authorization" -> s"Bearer $token")
        .get()
        .map { response =>
          (response.json \\ "classes")
        }

      val keywords = Await.result(keywordsFuture, Duration.Inf)
      val data = keywords(0).as[List[String]] mkString (" ")

      val resultFuture = videoDAO.updateImageData(id, frame.toLong, data)
      val result = Await.result(resultFuture, Duration.Inf)
      sender() ! result
  }
}
