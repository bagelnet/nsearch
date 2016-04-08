package models

import play.api.libs.json.Json

case class Content(meta: ContentInfo, data: Option[Array[Video]])

case class ContentInfo(status: Int, totalCount: Option[Int], errorCode: Option[String])

case class Video(contentId: String, title: String)

object Content {
  implicit def jsonWrites = Json.writes[Content]
  implicit def jsonReads = Json.reads[Content]
}