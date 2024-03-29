package controllers

import play.api.mvc._
import play.api.libs.ws._
import javax.inject._
import dao._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import scala.concurrent.{Future, Await}

import play.api.Logger

import java.io.{PipedInputStream, PipedOutputStream}
import play.api.libs.iteratee.Enumerator

import models.VideoMetadata
import play.api.http.HttpFilters
import play.filters.cors.CORSFilter

import akka.actor._
import scala.concurrent.duration._
import akka.pattern.ask
import actors.processors._
import actors.ImageProcessingActor._

import play.api.libs.json.Json
import play.api.libs.json.JsValue

import scala.util.{Left, Right, Success, Failure}

import scala.io.Source

import util.SRTParser._
import util.Fetch._

import sys.process._

@Singleton
class VideoController @Inject() (corsFilter: CORSFilter, videoDAO: VideoDAO, youtubeDAO: VideoMetadataDAO, system: ActorSystem, ws: WSClient) extends Controller {
  def filters = Seq(corsFilter)

  val tesseractProcessors = system.actorOf(Props(new TesseractProcessingActor(videoDAO)), "tesseract-processor")
  val clarifaiProcessors = system.actorOf(Props(new ClarifaiProcessingActor(ws, videoDAO)), "clarifai-processor")
  implicit val timeout = akka.util.Timeout(500000 seconds)

  def process(id: String) = Action {
    val progressProducer = new PipedOutputStream()
    val progressConsumer = new PipedInputStream(progressProducer)

    Logger.info(s"Video $id has begun processing")

    Future {
      val fetchData = fetch(id)
      val frames = (fetchData \ "num_frames").as[Long]
      val total_frames = (fetchData \ "total_frames").as[Long]
      val duration = (fetchData \ "duration").as[Int]
      val downloaded = (fetchData \ "downloaded_caption").as[Boolean]

      Logger.info(s"Video $id has been fetched and frames generated")

      // Publish the overall length of the video that needs to be processed
      progressProducer.write(frames.toInt * 2)
      progressProducer.flush()

      // Insert Video Metadata
      youtubeDAO.insert(VideoMetadata(id, total_frames, duration))

      Logger.info(s"Beginning processing captions for video $id")

      // Process Caption Data
      if (downloaded) {
        val text = Source.fromFile(s"/var/www/videos/$id.en.srt").mkString
        val captions: List[Caption] = parseSRT(text)
        captions.par.map { caption =>
          videoDAO.updateCaption(id, caption.getFrame(total_frames, duration), caption.content.toLowerCase())
        }
      }

      Logger.info(s"Beginning processing image data for video $id")

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

      val clarifaiProcs = (1 to frames.toInt).map(frame =>
        Logger.info(s"Classifying frame $frame.toString for video $id")
        (clarifaiProcessors ? ClarifaiImage(id, frame.toString, token, progressProducer)).mapTo[Boolean]
      )

      Logger.info(s"Beginning processing ocr for video $id")

      // Process Tesseract Data
      val tessProcs = (1 to frames.toInt).map(frame =>
        Logger.info(s"Running OCR on frame $frame.toString for video $id")
        (tesseractProcessors ? ProcessImage(id, frame.toString, progressProducer)).mapTo[Boolean]
      )

      Await.result(Future.sequence(tessProcs), Duration.Inf)
      Await.result(Future.sequence(clarifaiProcs), Duration.Inf)

      progressProducer.close()
    } onComplete {
      case Success(_) => Logger.info(s"Video $id has finished processing")
      case Failure(err) =>
        Logger.error(s"Video $id has failed", err)
        progressProducer.close()
        progressConsumer.close()
    }
    val dataContent: Enumerator[Array[Byte]] = Enumerator.fromStream(progressConsumer)
    Ok.chunked(dataContent)
  }

  def exists(id: String) = Action.async {
    Logger.info(s"Checking for video $id metadata")
    youtubeDAO.exists(id).map{
      case true => Ok
      case false => BadRequest
    }
  }

  def query(id: String, query: String) = Action.async {
    Logger.info(s"Querying video $id with query string: $query")
    youtubeDAO.fetch(id).map {
      case Left(metadata: VideoMetadata) =>
        val resultFuture = videoDAO.fetch(metadata, query)
        val result = Await.result(resultFuture, Duration.Inf)
        Ok(Json.toJson(result))
      case Right(err) => BadRequest(err)
    }
  }

}
