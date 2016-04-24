package controllers

import java.awt.Graphics
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.imageio.ImageIO

import play.api.mvc.{Controller, Action}

class ImageController extends Controller {

  def index(ids: String, ext: String) = Action {
    val src = ImageIO.read(new URL("http://tn-skr4.smilevideo.jp/smile?i=28549395"))
    val img:BufferedImage = new BufferedImage(src.getWidth() * 2, src.getHeight(), BufferedImage.TYPE_INT_ARGB);
    val base: Graphics = img.getGraphics()
    base.drawImage(src, 0, 0, null)
    base.drawImage(src, src.getWidth, 0, null)

    val stream: ByteArrayOutputStream = new ByteArrayOutputStream();
    ImageIO.write(img, "png", stream)
    Ok(stream.toByteArray()).as("image/png")
  }
}
