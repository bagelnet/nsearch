package controllers

import java.io.File

import com.typesafe.config.ConfigFactory
import models.Content
import play.api.libs.json.JsResult
import play.api.libs.ws.WSConfigParser
import play.api.libs.ws.ahc.{AhcConfigBuilder, AhcWSClient, AhcWSClientConfig}
import play.api.{Configuration, Environment, Mode}

import scala.concurrent.Future

class SearchController(url: String) {
  val configuration = Configuration.reference ++ Configuration(ConfigFactory.parseString(
    """
      |ws.useragent = "nsearch"
    """.stripMargin))
  val environment = Environment(new File("."), this.getClass.getClassLoader, Mode.Prod)

  val parser = new WSConfigParser(configuration, environment)
  val config = new AhcWSClientConfig(wsClientConfig = parser.parse())
  val builder = new AhcConfigBuilder(config)
//  val logging = new AsyncHttpClientConfig.AdditionalChannelInitializer() {
//    override def initChannel(channel: io.netty.channel.Channel): Unit = {
//      channel.pipeline.addFirst("log", new io.netty.handler.logging.LoggingHandler("debug"))
//    }
//  }
  val ahcBuilder = builder.configure()
//  ahcBuilder.setHttpAdditionalChannelInitializer(logging)
  val ahcConfig = ahcBuilder.build()
  val wsClient = new AhcWSClient(ahcConfig)

  def search(params: Map[String, Any]): Unit = {
    request(params) map {
      res => if (isError(res.get.meta)) printError() else printResult(res.get)
    }
  }

  private def request(params: Map[String, Any]): Future[JsResult[Content]] = {
    wsClient.url(url).withBody(Map("param1" -> Seq("value1"))).get().map {
      response => response.json.validate[Content]
    }
  }

  private def isError(meta: Object): Boolean = {
    true
  }

  def printError() = {
    println("Error!!!!")
  }

  def printResult(contents: Content): Unit = {
    println(contents.meta.totalCount)
    contents.data.get.foreach {
      video => println(video.contentId + ":" + video.title)
    }
  }



//  private def parseJson(api_result: String): JsResult[Content] = {
//    val json: JsValue = Json.parse(
//      """
//      | {"meta":{"status":200,"totalCount":4190},"data":[
//      | {"view_counter":1027141,"title":"【初音ミク】みくみくにしてあげる♪【してやんよ】","is_sp_ok":true,"movie_type":"flv"},{"view_counter":772611,"title":"VOCALOID2 初音ミクに「Ievan Polkka」を歌わせてみた","is_sp_ok":true,"movie_type":"flv"},
//      | {"view_counter":341103,"title":"初音ミクが可愛すぎるので描いてみた","is_sp_ok":true,"movie_type":"flv"},{"view_counter":269682,"title":"【初音ミク】恋スルVOC@LOID （修正版）【オリジナル曲】","is_sp_ok":true,"movie_type":"flv"},
//      | ]}
//    """.
//        stripMargin
//    )
//    json.validate[Content]
//  }
}

