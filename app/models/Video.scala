package models;

import play.api.libs.json.Json

case class Video(
  id: String,
  frame: Long,
  caption: Option[String],
  ocrData: Option[String],
  imageData: Option[String]
)

object Video {
  implicit val userFormat = Json.format[Video]
}
