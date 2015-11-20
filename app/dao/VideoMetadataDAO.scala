package dao

import javax.inject.{Singleton, Inject}
import scala.concurrent.Future
import models.VideoMetadata
import play.api.db.slick.HasDatabaseConfig
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import slick.driver.JdbcProfile

import scala.util.{Left, Right, Success, Failure}

import org.postgresql.util.PSQLException

trait VideoMetadataComponent { self: HasDatabaseConfig[JdbcProfile] =>
  import driver.api._

  class VideoMetadatas(tag: Tag) extends Table[VideoMetadata](tag, "inq_youtube_metadata") {
    def id = column[String]("id", O.PrimaryKey)
    def frames = column[Long]("frames")
    def duration = column[Int]("duration")
    def * = (id, frames, duration) <> ((VideoMetadata.apply _).tupled, VideoMetadata.unapply _)
  }
}

@Singleton()
class VideoMetadataDAO extends GenericDAO with VideoMetadataComponent {
  import driver.api._

  val videos = TableQuery[VideoMetadatas]

  def insert(metadata: VideoMetadata): Future[Boolean] = {
    val action = (videos returning videos.map(_.id)) += metadata
    db.run(action.asTry).map{
      case Success(_) => true
      case Failure(_) => false
    }
  }

}
