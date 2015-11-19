package actors

object ImageProcessingActor {
  case class ProcessImage(id: String, frame: String)
  case class ClarifaiImage(id: String, frame: String, token: String)
}
