package controllers

import play.api.mvc._
import play.api.libs.ws._
import javax.inject._
import dao._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.{Future, Await}

import models.VideoMetadata

import akka.actor._
import scala.concurrent.duration._
import akka.pattern.ask
import actors.processors._
import actors.ImageProcessingActor._
import actors.CaptionProcessingActor._

import scala.collection.mutable.ListBuffer

import play.api.libs.json.JsValue

@Singleton
class VideoController @Inject() (videoDAO: VideoDAO, metadataDAO: VideoMetadataDAO, system: ActorSystem, ws: WSClient) extends Controller {
  val tesseractProcessors = system.actorOf(Props(new TesseractProcessingActor(videoDAO)), "tesseract-processor")
  val clarifaiProcessors = system.actorOf(Props(new ClarifaiProcessingActor(ws, videoDAO)), "clarifai-processor")
  // val captionProcessors = system.actorOf(Props(new CaptionProcessingActor(videoDAO)), "caption-processor")

  implicit val timeout = akka.util.Timeout(500000 seconds)

  def testClarifai(id: String, frame: String) = Action {
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

    val url = s"https://api.clarifai.com/v1/tag/?url=http://104.236.166.190/frames/$id/$frame.jpg"
    val keywordsFuture: Future[Seq[JsValue]] = ws.url(url)
      .withHeaders("Authorization" -> s"Bearer $token")
      .get()
      .map { response =>
        (response.json \\ "classes")
      }

    val keywords = Await.result(keywordsFuture, Duration.Inf)
    println(keywords(0).as[List[String]])
    Ok("HI")
  }

  def process(id: String) = Action {
    val fetchFuture: Future[JsValue] = ws.url(s"http://localhost:8000/fetch/$id").get().map { response =>
      response.json
    }
    val fetchData = Await.result(fetchFuture, Duration.Inf)
    val frames = (fetchData \ "num_frames").as[Long]
    val total_frames = (fetchData \ "total_frames").as[Long]
    val duration = (fetchData \ "duration").as[Int]

    metadataDAO.insert(VideoMetadata(id, total_frames, duration))

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
