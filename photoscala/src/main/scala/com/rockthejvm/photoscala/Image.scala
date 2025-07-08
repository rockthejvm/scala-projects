package com.rockthejvm.photoscala

import java.awt.Graphics
import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

class Image private (private val buffImage: BufferedImage) {
  val width = buffImage.getWidth
  val height = buffImage.getHeight

  def getColor(x: Int, y: Int): Pixel =
    Pixel.fromHex(buffImage.getRGB(x, y))

  def setColor(x: Int, y: Int, p: Pixel): Unit =
    buffImage.setRGB(x, y, p.toInt)

  def save(path: String): Unit =
    ImageIO.write(buffImage, "JPG", new File(path))

  def saveResource(path: String): Unit =
    save(s"photoscala/src/main/resources/$path")

  def crop(startX: Int, startY: Int, w: Int, h: Int): Image = {
    assert(
      startX >= 0 &&
        startY >= 0 &&
        w > 0 && h > 0 &&
        startX + w < width && startY + h < height
    )

    val newPixels = Array.fill(w * h)(0)
    buffImage.getRGB(startX, startY, w, h, newPixels,0,w)
    val newBuffImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB)
    newBuffImage.setRGB(0,0,w,h,newPixels,0,w)
    new Image(newBuffImage)
  }

  def map(f: Pixel => Pixel): Image = {
    val newPixels = Array.fill(width * height)(0)
    buffImage.getRGB(0, 0, width, height, newPixels, 0, width)
    newPixels.mapInPlace { color =>
      val pixel = Pixel.fromHex(color)
      val newPixel = f(pixel)
      newPixel.toInt
    }

    val newBuffImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    newBuffImage.setRGB(0,0,width,height,newPixels,0,width)
    new Image(newBuffImage)
  }

  /*
     +-------------------------------------------------------+
     |                                                       |
     |           a1 a2 a3 a4 a5                              |
     |           b1 b2 b3 b4 b5                              |
  y >|           c1 c2 XX c4 c5                              |
     |           d1 d2 d3 d4 d5                              |
     |           e1 e2 e3 e4 e5                              |
     |                                                       |
     |                                                       |
     |                                                       |
     |                                                       |
     +-------------------------------------------------------+
                        ^
                        x
    window = [a1,a2,a3,a4,a5,b1,b2, ... ]
   */
  def window(x: Int, y: Int, width: Int, height: Int): Window = {
    val offsetX = (width - 1) / 2
    val offsetY = (height - 1) / 2
    val horizCoords = ((x - offsetX) to (x + offsetX))
      .map { v =>
        if v < 0 then 0
        else if v >= this.width then this.width - 1
        else v
      }

    val vertCoords = ((y - offsetY) to (y + offsetY))
      .map { v =>
        if v < 0 then 0
        else if v >= this.height then this.height - 1
        else v
      }

    val pixels = for {
      xp <- horizCoords
      yp <- vertCoords
    } yield getColor(xp, yp)

    Window(width, height, pixels.toList)
  }
  
  def draw(g: Graphics): Unit =
    g.drawImage(buffImage,0,0,null)
}

object Image {
  def apply(width: Int, height: Int, pixels: Array[Pixel]): Image = {
    val newBuffImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    newBuffImage.setRGB(0, 0, width, height, pixels.map(_.toInt), 0, width)
    new Image(newBuffImage)
  }
  
  def black(width: Int, height: Int): Image = {
    val colors = Array.fill(width * height)(0)
    val bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    bufferedImage.setRGB(0,0,width,height, colors, 0,width)
    new Image(bufferedImage)
  }

  def load(path: String): Image =
    new Image(ImageIO.read(new File(path)))

  def loadResource(path: String): Image =
    load(s"photoscala/src/main/resources/$path")

  def main(args: Array[String]): Unit = {
    loadResource("metal.png").crop(0,0,600,600).saveResource("metal_cropped.jpg")
  }
}
