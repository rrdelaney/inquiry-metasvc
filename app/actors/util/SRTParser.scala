package actors.util

class SRTParser {
  val captionRegex = """\d+\n(\d{2})\:(\d{2})\:(\d{2})\,(\d{3}) --> (\d{2})\:(\d{2})\:(\d{2})\,(\d{3})\n(.*)""".r

  case class Caption(
      startHour: Int,
      startMinute: Int,
      startSecond: Int,
      endHour: Int,
      endMinute: Int,
      endSecond: Int,
      val content: String
  ) {
      val startTime = (3600 * startHour) + (60 * startMinute) + startSecond
      val endTime = (3600 * endHour) + (60 * endMinute) + endSecond

      override def toString() = s"$startTime --> $endTime \n$content"
  }

  def parseSRT(input: String) =
      (for (
          c <- captionRegex findAllMatchIn input
      ) yield Caption(
          (c group 1).toInt,
          (c group 2).toInt,
          (c group 3).toInt,
          (c group 5).toInt,
          (c group 6).toInt,
          (c group 7).toInt,
          c group 9
      )).toList
}
