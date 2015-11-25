package dao

import javax.inject.{Singleton, Inject}
import scala.concurrent.Future
import models._
import play.api.db.slick.HasDatabaseConfig
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.util.{Left, Right, Success, Failure}

import org.postgresql.util.PSQLException

trait VideoComponent { self: HasDatabaseConfig[JdbcProfile] =>
  import driver.api._

  class Videos(tag: Tag) extends Table[Video](tag, "inq_video_metadata") {
    def id = column[String]("id", O.PrimaryKey)
    def frame = column[Long]("frame", O.PrimaryKey)
    def caption = column[Option[String]]("caption")
    def ocrData = column[Option[String]]("ocr_data")
    def imageData = column[Option[String]]("image_data")
    def * = (id, frame, caption, ocrData, imageData) <> ((Video.apply _).tupled, Video.unapply _)
  }
}

@Singleton()
class VideoDAO extends GenericDAO with VideoComponent {
  import driver.api._

  val videos = TableQuery[Videos]

  def fetch(metadata: VideoMetadata, query: String): Future[Seq[Long]] = {
    val id = metadata.id
    val frames = metadata.frames
    val duration = metadata.duration
    val keywords = s"%$query%"
    val action = sql"""SELECT FLOOR(frame) AS frames
                       FROM inq_video_metadata
                       WHERE id = $id
                       AND LOWER(image_data) LIKE $keywords
                       OR LOWER(ocr_data) LIKE $keywords
                       OR LOWER(caption) LIKE $keywords""".as[Long]

    db.run(action.asTry).map {
      case Success(data: Seq[Long]) => data
      case Failure(data) => Seq.empty[Long]
    }
  }

  def exists(id: String): Future[Boolean] = {
    val action = videos.filter(_.id === id).length
    db.run(action.result).map { result => result > 0 }
  }

  def updateCaption(id: String, frame: Long, caption: String): Future[Boolean] =  {
    val action = (
        for {
        existing <- videos.filter(_.id === id).filter(_.frame === frame).result.headOption
        row       = existing.map(_.copy(caption=Some(caption))) getOrElse Video(id, frame, Some(caption), None, None)
        result   <- videos.insertOrUpdate(row)
      } yield result
    ).transactionally

    db.run(action.asTry).map {
      case Success(_) => true
      case Failure(_) => false
    }
  }

  def updateOCRData(id: String, frame: Long, data: String): Future[Boolean] =  {
    val action = (
        for {
        existing <- videos.filter(_.id === id).filter(_.frame === frame).result.headOption
        row       = existing.map(_.copy(ocrData=Some(data))) getOrElse Video(id, frame, None, Some(data), None)
        result   <- videos.insertOrUpdate(row)
      } yield result
    ).transactionally

    db.run(action.asTry).map {
      case Success(_) => true
      case Failure(_) => false
    }
  }

  def updateImageData(id: String, frame: Long, data: String): Future[Boolean] =  {
    val action = (
        for {
        existing <- videos.filter(_.id === id).filter(_.frame === frame).result.headOption
        row       = existing.map(_.copy(imageData=Some(data))) getOrElse Video(id, frame, None, None, Some(data))
        result   <- videos.insertOrUpdate(row)
      } yield result
    ).transactionally

    db.run(action.asTry).map {
      case Success(_) => true
      case Failure(_) => false
    }
  }

}
