package models;

import play.api.libs.json.Json

case class VideoMetadata(
  id: String,
  frames: Long,
  duration: Int
)

object VideoMetadata {
  implicit val userFormat = Json.format[VideoMetadata]
}
