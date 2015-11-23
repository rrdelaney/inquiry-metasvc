package util

object SRTParser {
  val captionRegex = """\d+\n(\d{2})\:(\d{2})\:(\d{2})\,(\d{3}) --> (\d{2})\:(\d{2})\:(\d{2})\,(\d{3})\n([\s\S]*?)\n{2}""".r

  case class Caption(
      startHour: Int,
      startMinute: Int,
      startSecond: Int,
      val content: String
  ) {
      val startTime = (3600 * startHour) + (60 * startMinute) + startSecond

      def getFrame(frames: Long, duration: Int) = startTime * frames / (24 * duration)
  }

  def parseSRT(input: String) =
      (for (
          c <- captionRegex findAllMatchIn input
      ) yield Caption(
          (c group 1).toInt,
          (c group 2).toInt,
          (c group 3).toInt,
          c group 9
      )).toList
}
