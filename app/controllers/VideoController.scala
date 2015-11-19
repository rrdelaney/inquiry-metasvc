package controllers

import play.api.mvc._
import play.api.libs.ws._
import javax.inject._
import dao.VideoDAO
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.{Future, Await}

import akka.actor._
import scala.concurrent.duration._
import akka.pattern.ask
import actors.processors._
import actors.ImageProcessingActor._
import actors.CaptionProcessingActor._

import scala.collection.mutable.ListBuffer

@Singleton
class VideoController @Inject() (videoDAO: VideoDAO, system: ActorSystem, ws: WSClient) extends Controller {
  val tesseractProcessors = system.actorOf(Props(new TesseractProcessingActor(videoDAO)), "tesseract-processor")
  val clarifaiProcessors = system.actorOf(Props(new ClarifaiProcessingActor(ws, videoDAO)), "clarifai-processor")
  // val captionProcessors = system.actorOf(Props(new CaptionProcessingActor(videoDAO)), "caption-processor")

  implicit val timeout = akka.util.Timeout(500000 seconds)

  def process(id: String) = Action {
    val framesFuture: Future[Long] = ws.url(s"http://localhost:8000/fetch/$id").get().map { response =>
      (response.json \ "num_frames").as[Long]
    }
    val frames = Await.result(framesFuture, Duration.Inf)

    // Process Tesseract Data
    val tessProcs = (1 to frames.toInt).map(frame => (tesseractProcessors ? ProcessImage(id, frame.toString)).mapTo[Boolean])

    // Get Clarifai OAuth2 Token
    val tokenFuture: Future[String] =
      ws.url("https://api.clarifai.com/v1/token/")
        .post(Map(
          "grant_type" -> Seq("client_credentials"),
          "client_id" -> Seq("vxgYA0F1qWmZQktMmKMhppqIQss4zMYDmxQX3kbD"),
          "client_secret" -> Seq("ipFs34CvymKN8sXYHH3Rph1G7QIELIXwhFR4b8eq")
        ))
        .map { response =>
          (response.json \ "access_token").as[String]
        }

    val token: String = Await.result(tokenFuture, Duration.Inf)

    val clarifaiProcs = (1 to frames.toInt).map(frame => (clarifaiProcessors ? ClarifaiImage(id, frame.toString, token)).mapTo[Boolean])
    // val captionProc = (captionProcessors ? ProcessCaption(id)).mapTo[Boolean]

    Await.result(Future.sequence(tessProcs), Duration.Inf)
    Await.result(Future.sequence(clarifaiProcs), Duration.Inf)
    Ok("Sent Messages Bruh")
  }

  def exists(id: String) = Action.async {
    videoDAO.exists(id).map{
      case true => Ok("Video Exists")
      case false => BadRequest("Video Does Not Exist")
    }
  }

}
