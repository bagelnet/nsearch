package controllers

import com.google.inject.Inject
import models.{Video, ContentInfo, Content}
import play.api.libs.json.JsResult
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

class SearchController @Inject() (ws: WSClient) extends Controller {

  def index = Action.async {
    val params = Map("q" -> "test", "_sort" -> "viewCounter", "targets" -> "tagsExact", "fields" -> "contentId,title", "_context" -> "nsearch")
    val response = search(params)
    var totalCount: Int = 0
    var videoList: List[Video] = List()
    Future.firstCompletedOf(Seq(response)) map {
      res =>
        if (res.isDefined && !isError(res.get.get.meta)) {
          totalCount = res.get.get.meta.totalCount.get
          videoList = res.get.get.data.get.toList
        } else {
          totalCount = -1
        }
        Ok(views.html.index("keyword", totalCount, videoList))
    }
  }

  def search(params: Map[String, String]): Future[Option[JsResult[Content]]]  = {
    val baseUrl = "http://api.search.nicovideo.jp/api/v2/video/contents/search"
    ws.url(baseUrl).withQueryString(params.toList: _*).get().map {
      response => {
        Some(response.json.validate[Content])
      }
    }.recover {
      case e: Exception => None
    }
  }

  private def isError(meta: ContentInfo): Boolean = {
    if (meta.status != 200) true else false
  }
}

