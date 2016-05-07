package models

import play.api.libs.json.Json

case class Content(meta: ContentInfo, data: Option[Array[Video]])
case class ContentVideo(meta: ContentInfo, data: Option[Array[Video]])
case class ContentLive(meta: ContentInfo, data: Option[Array[Live]])
case class ContentIllust(meta: ContentInfo, data: Option[Array[Illust]])

case class ContentInfo(status: Int, totalCount: Option[Int], errorCode: Option[String])

case class Video (contentId: String, title: String, tags: String, viewCounter: Int, commentCounter: Int, startTime: String, thumbnailUrl: String, mylistCounter: Int, lengthSeconds: Int)
case class Live  (contentId: String, title: String, tags: String, viewCounter: Int, commentCounter: Int, startTime: String, thumbnailUrl: String)
case class Illust(contentId: String, title: String, tags: String, viewCounter: Int, commentCounter: Int, startTime: String, thumbnailUrl: String, mylistCounter: Int)

object Content {
  implicit val contentInfoFormat = Json.format[ContentInfo]
  implicit val videoFormat = Json.format[Video]
  implicit val contentFormat = Json.format[Content]
}
object ContentVideo {
  implicit val contentInfoFormat = Json.format[ContentInfo]
  implicit val videoFormat = Json.format[Video]
  implicit val contentVideoFormat = Json.format[ContentVideo]
}
object ContentLive {
  implicit val contentInfoFormat = Json.format[ContentInfo]
  implicit val videoFormat = Json.format[Live]
  implicit val contentVideoFormat = Json.format[ContentLive]
}
object ContentIllust {
  implicit val contentInfoFormat = Json.format[ContentInfo]
  implicit val videoFormat = Json.format[Illust]
  implicit val contentVideoFormat = Json.format[ContentIllust]
}