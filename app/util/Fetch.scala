package util

import play.api._
import play.api.mvc._
import scala.sys.process._
import play.api.libs.json._

import java.io.File

object Fetch {

   def downloadVideo(video_id : String): String = {
      val link: String = s"https://www.youtube.com/watch?v=$video_id"
      val download_string = s"youtube-dl --write-auto-sub --write-srt --sub-lang en -o /var/www/videos/$video_id.%(ext)s $link"
      download_string !

      val extensionSearch = "ls -l /var/www/videos" #| s"grep $video_id" !!

      val extensionRegex = """\.([^(en\.srt)]\w+)""".r
      val extension: Option[String] = for (
         c <- extensionRegex findFirstMatchIn extensionSearch
      ) yield (c group 1)

      extension match {
         case Some(ext) => ext
         case None => ""
      }
   }

   def getDuration(video_id: String, extension: String): Float = {
     val duration = s"ffprobe -v error -show_entries format=duration -of default=noprint_wrappers=1:nokey=1 /var/www/videos/$video_id.$extension" !!

     duration.toFloat
   }

   def extractFrames(video_id : String, extension: String) {
      s"mkdir /var/www/frames/$video_id" !
      val frames = s"ffmpeg -i /var/www/videos/$video_id.$extension -r 2 /var/www/frames/$video_id/%d.png" !!
   }

   def numFrames(video_id: String): Int = {
      val frames: String = s"ls /var/www/frames/$video_id" #| "wc -l" !!

      frames.trim.toInt
   }

   def totalFrames(video_id: String, duration: Float, extension: String): Float = {
      val fps = s"ffprobe /var/www/videos/$video_id.$extension -v 0 -select_streams v -print_format flat -show_entries stream=r_frame_rate" !!

      val fpsRegex = """(\d+)/(\d+)""".r
      val fpsReal: Option[Float] = for (
         c <- fpsRegex findFirstMatchIn fps
      ) yield ((c group 1).toFloat / (c group 2).toFloat)

      fpsReal match {
         case Some(f) => f * duration
         case None => 0
      }
   }

   def fetch(video_id : String) = {
      val extension = downloadVideo(video_id)
      val duration = getDuration(video_id, extension)
      extractFrames(video_id, extension)
      val frames = numFrames(video_id)
      val total_frames = totalFrames(video_id, duration, extension)
      val cap_exists = new java.io.File(s"/var/www/videos/$video_id.en.srt").exists

      Json.obj(
        "num_frames" -> frames,
        "total_frames" -> total_frames.toInt,
        "duration" -> duration.toInt,
        "downloaded_caption" -> cap_exists
      )
   }
}
