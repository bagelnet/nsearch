package controllers

import java.util.Optional

import com.google.inject.Inject
import models._
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

  def top = Action.async {
    val params: Seq[Map[String, Map[String, String]]] = Seq(
      Map(Services.video -> getAPIParamsDefault),
      Map(Services.live -> getAPIParamsLiveDefault),
      Map(Services.illust -> getAPIParamsIllustDefault))
    val fs: Seq[Future[Option[Map[String,JsResult[Any]]]]] = params.map(p => search(p.head._1, p.head._2))
    var resultVideo: List[Video] = List()
    var resultLive: List[Live] = List()
    var resultIllust: List[Illust] = List()
    Future.sequence(fs).map { responses =>
      responses.map { response =>
        response match {
          case res: Option[Map[String,JsResult[ContentVideo]]] if (res.isDefined && res.get.head._1 == "video") => if(!isError(res.get.head._2.get.meta)) resultVideo = res.get.head._2.get.data.get.toList
          case res: Option[Map[String,JsResult[ContentLive]]] if (res.isDefined && res.get.head._1 == "live")  => if(!isError(res.get.head._2.get.meta)) resultLive = res.get.head._2.get.data.get.toList
          case res: Option[Map[String,JsResult[ContentIllust]]] if (res.isDefined && res.get.head._1 == "illust") => if(!isError(res.get.head._2.get.meta)) resultIllust = res.get.head._2.get.data.get.toList
          case _ => throw new ClassCastException
        }
      }
      Ok(views.html.top(resultVideo, resultLive, resultIllust))
    }.recover {
      case e: Exception => Ok(views.html.top(resultVideo, resultLive, resultIllust))
    }
  }

  def video = Action.async { implicit request =>
    val form = Form(mapping("q" -> text, "t" -> list(text), "s" -> text, "r" -> optional(text), "p" -> optional(text))(Param.apply)(Param.unapply))
    var params: Map[String, String] = getAPIParamsDefault
    form.bindFromRequest.fold(
      errors => {
        // use default params for initial access or parameter error
      },
      message => {
        params = createAPIParams(message, params)
      }
    )

    val response = search(Services.video, params)
    var totalCount: Int = 0
    var videoList: List[Video] = List()
    Future.firstCompletedOf(Seq(response)) map {
      response =>
        response match {
          case res: Option[Map[String,JsResult[ContentVideo]]] => {
            if (res.isDefined && !isError(res.get.head._2.get.meta)) {
              totalCount = res.get.head._2.get.meta.totalCount.get
              videoList = res.get.head._2.get.data.get.toList
            } else {
              totalCount = -1
            }
            Ok(views.html.index(params, totalCount, videoList, request.host, getThumbnailName(videoList)))
          }
        }
    }
  }

  def live = Action.async { implicit request =>
    val form = Form(mapping("q" -> text, "t" -> list(text), "s" -> text, "r" -> optional(text), "p" -> optional(text))(Param.apply)(Param.unapply))
    var params: Map[String, String] = getAPIParamsLiveDefault
    form.bindFromRequest.fold(
      errors => {
        // use default params for initial access or parameter error
      },
      message => {
        params = createAPIParams(message, params)
      }
    )

    val response = search(Services.live, params)
    var totalCount: Int = 0
    var videoList: List[Live] = List()
    Future.firstCompletedOf(Seq(response)) map {
      response =>
        response match {
          case res: Option[Map[String,JsResult[ContentLive]]] => {
            if (res.isDefined && !isError(res.get.head._2.get.meta)) {
              totalCount = res.get.head._2.get.meta.totalCount.get
              videoList = res.get.head._2.get.data.get.toList
            } else {
              totalCount = -1
            }
            Ok(views.html.live(params, totalCount, videoList, request.host, ""))
          }
        }
    }
  }

  def illust = Action.async { implicit request =>
    val form = Form(mapping("q" -> text, "t" -> list(text), "s" -> text, "r" -> optional(text), "p" -> optional(text))(Param.apply)(Param.unapply))
    var params: Map[String, String] = getAPIParamsIllustDefault
    form.bindFromRequest.fold(
      errors => {
        // use default params for initial access or parameter error
      },
      message => {
        params = createAPIParams(message, params)
      }
    )

    val response = search(Services.illust, params)
    var totalCount: Int = 0
    var videoList: List[Illust] = List()
    Future.firstCompletedOf(Seq(response)) map {
      response =>
        response match {
          case res: Option[Map[String,JsResult[ContentIllust]]] => {
            if (res.isDefined && !isError(res.get.head._2.get.meta)) {
              totalCount = res.get.head._2.get.meta.totalCount.get
              videoList = res.get.head._2.get.data.get.toList
            } else {
              totalCount = -1
            }
            Ok(views.html.illust(params, totalCount, videoList, request.host, ""))
          }
        }
    }
  }

  def search(service: String, params: Map[String, String])  = {
    val baseUrl = s"http://api.search.nicovideo.jp/v2/${service}/contents/search"
    ws.url(baseUrl).withQueryString(params.toList: _*).get().map {
      response => {
        service match {
          case "video" => Some(Map(service -> response.json.validate[ContentVideo]))
          case "live" => Some(Map(service -> response.json.validate[ContentLive]))
          case "illust" => Some(Map(service -> response.json.validate[ContentIllust]))
        }
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
    "q" -> "",
    "targets" -> "tagsExact",
    "fields" -> "contentId,title,tags,viewCounter,mylistCounter,commentCounter,startTime,thumbnailUrl,lengthSeconds",
    "filters[startTime][gte]" -> new DateTime().minusDays(7).toString("yyyy-MM-dd'T'HH:mm:ssZ"),
    "filters[lengthSeconds][gte]" -> "0",
    "filters[lengthSeconds][lte]" -> "6000",
    "_sort" -> "-viewCounter",
    "_context" -> "nsearch")
  }

  private def getAPIParamsLiveDefault: Map[String, String] = {
    Map(
      "q" -> "",
      "targets" -> "tagsExact",
      "fields" -> "contentId,title,tags,viewCounter,commentCounter,startTime,thumbnailUrl",
      "filters[startTime][gte]" -> new DateTime().minusDays(1).toString("yyyy-MM-dd'T'HH:mm:ssZ"),
      "filters[-title][0]" -> "ニコ生クルーズ",
      "_sort" -> "-viewCounter",
      "_context" -> "nsearch")
  }

  private def getAPIParamsIllustDefault: Map[String, String] = {
    Map(
      "q" -> "",
      "targets" -> "tagsExact",
      "fields" -> "contentId,title,tags,viewCounter,commentCounter,startTime,thumbnailUrl,mylistCounter",
      "filters[startTime][gte]" -> new DateTime().minusDays(7).toString("yyyy-MM-dd'T'HH:mm:ssZ"),
      "_sort" -> "-viewCounter",
      "_context" -> "nsearch")
  }

  private def createAPIParams(message: Param, default: Map[String, String]): Map[String, String] = {
    var params = default
    params foreach {
      case ("q", v) => params = params.updated("q", getParamsQuery(message.q).getOrElse(v))
      case ("targets", v) => params = params.updated("targets", getParamsTargets(message.t).getOrElse(v))
      case ("filters[startTime][gte]", v) => params = params.updated("filters[startTime][gte]", getParamsTimeFilters(message.r.getOrElse(v)).getOrElse(v))
      case ("filters[lengthSeconds][gte]", v) => params = params.updated("filters[lengthSeconds][gte]", getParamsLengthFilters(message.p.getOrElse(""), "min").getOrElse(v))
      case ("filters[lengthSeconds][lte]", v) => params = params.updated("filters[lengthSeconds][lte]", getParamsLengthFilters(message.p.getOrElse(""), "max").getOrElse(v))
      case ("_sort", v) => params = params.updated("_sort", getParamsSort(message.s).getOrElse(v))
      case ("fields", v) =>
      case ("_context", v) =>
      case _ =>
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

  private def getParamsLengthFilters(p: String, t: String): Option[String] = {
    if (p.isEmpty) None
    try {
      val Array(min, max)  = p.split(',')
      t match {
        case "min" => if (min forall { _.isDigit }) Some((min.toInt*60).toString) else None
        case "max" => if (max forall { _.isDigit }) Some((max.toInt*60).toString) else None
        case _ => None
      }
    } catch {
      case e: Exception => None
    }
  }

  private def getParamsSort(s: String): Option[String] = {
    if (s.length != 2) return None
    val sort: String = (s collect {
      case 'a' => "+"
      case 'd' => "-"
      case 'l' => "liveRecent"
      case 'm' => "mylistCounter"
      case 'p' => "viewCounter"
      case 'r' => "startTime"
    }).mkString("")
    if (sort.matches("""[+\-][a-zA-Z]+[a-z]""")) Some(sort) else None
  }

  private def getThumbnailName(vList: List[Video]): String = {
    var urlList = List[String]()
    vList foreach {
      case v if !v.thumbnailUrl.isEmpty => urlList =  v.thumbnailUrl.get :: urlList
      case _ =>
    }
    var idList = List[String]()
    val Pattern = """http://[a-z\-]+([0-9]+).smilevideo.jp/smile\?i=([0-9]+)""".r
    urlList foreach {
      case Pattern(hostId, contentId) => idList = hostId + "_" + contentId :: idList
      case _ =>
    }
    idList.take(3).mkString("-") + ".png"
  }

  object Services {
    val video: String = "video"
    val live: String = "live"
    val illust: String = "illust"
    val news: String = "news"
  }
}

