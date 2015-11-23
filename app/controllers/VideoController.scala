package controllers

import play.api.mvc._
import play.api.libs.ws._
import javax.inject._
import dao._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.{Future, Await}

import models.VideoMetadata
import play.api.http.HttpFilters
import play.filters.cors.CORSFilter

import akka.actor._
import scala.concurrent.duration._
import akka.pattern.ask
import actors.processors._
import actors.ImageProcessingActor._

import scala.collection.mutable.ListBuffer
import play.api.libs.json.Json
import play.api.libs.json.JsValue

import scala.util.{Left, Right}

import scala.io.Source

import util.SRTParser._
import util.Fetch._

@Singleton
class VideoController @Inject() (corsFilter: CORSFilter, videoDAO: VideoDAO, metadataDAO: VideoMetadataDAO, system: ActorSystem, ws: WSClient) extends Controller {
  def filters = Seq(corsFilter)

  val tesseractProcessors = system.actorOf(Props(new TesseractProcessingActor(videoDAO)), "tesseract-processor")
  val clarifaiProcessors = system.actorOf(Props(new ClarifaiProcessingActor(ws, videoDAO)), "clarifai-processor")

  implicit val timeout = akka.util.Timeout(500000 seconds)

  def process(id: String) = Action {
    // val fetchFuture: Future[JsValue] = ws.url(s"http://localhost:8000/fetch/$id").get().map { response => response.json }
    // val fetchData = Await.result(fetchFuture, Duration.Inf)
    val fetchData = fetch(id)
    val frames = (fetchData \ "num_frames").as[Long]
    val total_frames = (fetchData \ "total_frames").as[Long]
    val duration = (fetchData \ "duration").as[Int]
    val downloaded = (fetchData \ "downloaded_caption").as[Boolean]

    // Insert Video Metadata
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

    if (downloaded) {
      val text = Source.fromFile(s"/var/www/videos/$id.en.srt").mkString
      val captions: List[Caption] = parseSRT(text)
      captions.par.map { caption =>
        videoDAO.updateCaption(id, caption.getFrame(total_frames, duration), caption.content.toLowerCase())
      }
    }

    Await.result(Future.sequence(tessProcs), Duration.Inf)
    Await.result(Future.sequence(clarifaiProcs), Duration.Inf)
    Ok
  }


  def exists(id: String) = Action.async {
    videoDAO.exists(id).map{
      case true => Ok
      case false => BadRequest
    }
  }

  def query(id: String, query: String) = Action.async {
    metadataDAO.fetch(id).map {
      case Left(metadata: VideoMetadata) =>
        val resultFuture = videoDAO.fetch(metadata, query)
        val result = Await.result(resultFuture, Duration.Inf)
        Ok(Json.toJson(result))
      case Right(err) => BadRequest(err)
    }
  }

}
