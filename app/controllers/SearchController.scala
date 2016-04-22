package controllers

import com.google.inject.Inject
import models.{Video, ContentInfo, Content, Param}
import org.joda.time.DateTime
import org.joda.time.format.DateTimeFormat
import play.api.data.Form
import play.api.data.Forms._
import play.api.libs.json.JsResult
import play.api.libs.ws.WSClient
import play.api.mvc.{Action, Controller}
import play.api.libs.concurrent.Execution.Implicits._

import scala.concurrent.Future

class SearchController @Inject() (ws: WSClient) extends Controller {

  def index = Action.async { implicit request =>
    val form = Form(mapping("q" -> text, "t" -> list(text), "s" -> text, "r" -> text, "p1" -> number, "p2" -> number)(Param.apply)(Param.unapply))
    var params: Map[String, String] = getAPIParamsDefault
    form.bindFromRequest.fold(
      errors => {
        // use default params for initial access or parameter error
      },
      message => {
        params = createAPIParams(message)
      }
    )

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
        Ok(views.html.index(params, totalCount, videoList))
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

  private def getAPIParamsDefault: Map[String, String] = {
    Map(
    "q" -> "ゲーム",
    "targets" -> "tagsExact",
    "fields" -> "contentId,title,tags,viewCounter,mylistCounter,commentCounter,startTime,thumbnailUrl,lengthSeconds",
    "filters[startTime][gte]" -> new DateTime().minusDays(30).toString("yyyy-MM-dd'T'HH:mm:ssZ"),
    "filters[lengthSeconds][gte]" -> "0",
    "filters[lengthSeconds][lte]" -> "6000",
    "_sort" -> "-viewCounter",
    "_context" -> "nsearch")
  }

  private def createAPIParams(message: Param): Map[String, String] = {
    var params = getAPIParamsDefault
    params foreach {
      case ("q", v) => params = params.updated("q", getParamsQuery(message.q).getOrElse(v))
      case ("targets", v) => params = params.updated("targets", getParamsTargets(message.t).getOrElse(v))
      case ("filters[startTime][gte]", v) => params = params.updated("filters[startTime][gte]", getParamsTimeFilters(message.r).getOrElse(v))
      case ("filters[lengthSeconds][gte]", v) => params = params.updated("filters[lengthSeconds][gte]", getParamsLengthFilters(message.p1).getOrElse(v))
      case ("filters[lengthSeconds][lte]", v) => params = params.updated("filters[lengthSeconds][lte]", getParamsLengthFilters(message.p2).getOrElse(v))
      case ("_sort", v) => params = params.updated("_sort", getParamsSort(message.s).getOrElse(v))
      case ("fields", v) =>
      case ("_context", v) =>
    }

    params
  }

  private def getParamsQuery(q: String): Option[String] = {
    if (q.isEmpty) None else Some(q)
  }

  private def getParamsTargets(t: List[String]): Option[String] = {
    val targets = t.filter(f => Set("title", "description", "tags", "tagsExact").contains(f)).mkString(",")
    if (targets.isEmpty) None else Some(targets)
  }

  private def getParamsTimeFilters(p: String): Option[String] = {
    try {
      Some(DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ").print(DateTimeFormat.forPattern("yyyy-MM-dd'T'HH").parseDateTime(p)))
    } catch {
      case e: Exception => None
    }
  }

  private def getParamsLengthFilters(p: Int): Option[String] = {
    if (p < 0) None else Some((p*60).toString)
  }

  private def getParamsSort(s: String): Option[String] = {
    if (s.length != 2) return None
    val sort: String = (s collect {
      case 'a' => "+"
      case 'd' => "-"
      case 'm' => "MylistCounter"
      case 'p' => "playCounter"
      case 'r' => "startTime"
    }).toString()
    if (sort.matches("""[+\-][a-zA-Z]+[a-z]""")) Some(sort) else None
  }
}

