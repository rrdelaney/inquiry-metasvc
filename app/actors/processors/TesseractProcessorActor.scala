package actors.processors

import akka.actor._
import play.api.libs.ws._
import scala.concurrent.Future

import dao.VideoDAO
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.duration.Duration
import scala.concurrent.Await
import sys.process._

class TesseractProcessingActor(videoDAO: VideoDAO) extends Actor {
  import actors.ImageProcessingActor._

  def receive = {
    case ProcessImage(id: String, frame: String) =>
      println(s"Processing Frame OCR Data : $frame" )
      val text: String = s"tesseract /var/www/frames/$id/$frame.png stdout" !!

      val resultFuture = videoDAO.updateOCRData(id, frame.toLong, text)
      val result = Await.result(resultFuture, Duration.Inf)
      println(s"Finished Frame OCR Data : $frame" )
      sender() ! result
  }
}
