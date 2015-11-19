package actors.processors

import akka.actor._
import play.api.libs.ws._
import scala.concurrent.Future

import dao.VideoDAO
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import sys.process._

class TesseractProcessingActor(videoDAO: VideoDAO) extends Actor {
  import actors.ImageProcessingActor._

  def receive = {
    case ProcessImage(id: String, frame: String) =>
      val text: String = s"tesseract /var/www/frames/$id/$frame.jpg stdout" !!
      val result = videoDAO.updateOCRData(id, frame.toLong, text).map { result =>
        sender() ! result
      }
  }
}
