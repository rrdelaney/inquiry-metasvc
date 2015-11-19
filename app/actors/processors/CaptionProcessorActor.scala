package actors.processors

import akka.actor._
import play.api.libs.ws._
import scala.concurrent.Future

import dao.VideoDAO

class CaptionProcessingActor(videoDAO: VideoDAO) extends Actor {
  import actors.CaptionProcessingActor._

  def receive = {
    case ProcessCaption(id: String) =>
      Thread.sleep(1000);
      println("Hello from caption processor")
      sender() ! true
  }
}
