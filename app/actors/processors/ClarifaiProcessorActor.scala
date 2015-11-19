package actors.processors

import akka.actor._
import play.api.libs.ws._
import scala.concurrent.Future

import dao.VideoDAO

import play.api.libs.concurrent.Execution.Implicits.defaultContext

class ClarifaiProcessingActor(ws: WSClient, videoDAO: VideoDAO) extends Actor {
  import actors.ImageProcessingActor._

  def receive = {
    case ClarifaiImage(id: String, frame: String, token: String) =>
      val url = s"https://api.clarifai.com/v1/tag/?url=http://104.236.166.190/frames/$id/$frame.jpg"
      val keywords = ws.url(url)
        .withHeaders("Authorization" -> s"Bearer $token")
        .get()
        .map { response =>
          (response.json \\ "classes")
        }
      sender() ! true
  }
}
