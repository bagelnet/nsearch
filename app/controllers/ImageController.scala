package controllers

import java.awt.Graphics
import java.awt.image.BufferedImage
import java.io.{File, ByteArrayOutputStream}
import java.net.URL
import javax.imageio.ImageIO

import play.api.mvc.{Controller, Action}

class ImageController extends Controller {

  def index(ids: String, ext: String) = Action {
    val width = 130
    val height = 100
    val maxWidth = 600
    val maxHeight = 315
    val cx = maxWidth * 3 / 5
    val cy = maxHeight / 3
    val xList: Seq[Int] = Seq(cx, cx - width, cx - width/3)
    val yList: Seq[Int] = Seq(cy - height, cy - height*3/4, cy - height/3)
    val idList: List[String] = getIdList(ids)
    val urlList: List[String] = getUrlList(idList).reverse
    val img = new BufferedImage(maxWidth, maxHeight, BufferedImage.TYPE_INT_ARGB)
    val base: Graphics = img.getGraphics()

    try {
      base.drawImage(ImageIO.read(new File("public/images/nsearch.png")), 0, 0, 600, 315, null)
      urlList.zipWithIndex foreach {
        case (url, i) => {
          base.drawImage(ImageIO.read(new URL(url)), xList(i), yList(i), null)
        }
      }
    } catch {
      case e:Exception => base.drawImage(ImageIO.read(new File("public/images/nsearch.png")), 0, 0, null)
    }

    val stream = new ByteArrayOutputStream()
    ImageIO.write(img, "png", stream)
    Ok(stream.toByteArray()).as("image/png")
  }

  private def getIdList(ids: String): List[String] = {
    if (ids.contains('-')) ids.split('-').toList
    else List[String]()
  }

  private def getUrlList(ids: List[String]): List[String] = {
    ids map {
      case id if id.contains('_') => genUrl(id.split('_'))
      case _ => ""
    }
  }

  private def genUrl(ids: Array[String]): String = {
    if (ids.length == 2) s"http://tn-skr${ids(0)}.smilevideo.jp/smile?i=${ids(1)}"
    else ""
  }
}
