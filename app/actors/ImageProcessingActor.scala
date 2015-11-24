package actors

import java.io.PipedOutputStream

object ImageProcessingActor {
  case class ProcessImage(id: String, frame: String, progress: PipedOutputStream)
  case class ClarifaiImage(id: String, frame: String, token: String, progress: PipedOutputStream)
}
