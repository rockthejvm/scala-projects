package com.rockthejvm.photoscala

import java.awt.image.BufferedImage
import java.io.File
import javax.imageio.ImageIO

// red/green/blue between 0-255
case class Pixel(private val red: Int, private val green: Int, private val blue: Int) {
  val r = Pixel.clamp(red)
  val g = Pixel.clamp(green)
  val b = Pixel.clamp(blue)

  // color: #12ffcb
  // r = 000000000000000000000000rrrrrrrr
  // g = 000000000000000000000000gggggggg
  // b = 000000000000000000000000bbbbbbbb
  // n = 00000000rrrrrrrrggggggggbbbbbbbb

  def toInt: Int =
    (r << 16) | (g << 8) | b

  infix def +(other: Pixel): Pixel =
    Pixel(
      Pixel.clamp(r + other.r),
      Pixel.clamp(g + other.g),
      Pixel.clamp(b + other.b)
    )

  def draw(width: Int, height: Int, path: String) = {
    val color = toInt
    val image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    val pixels = Array.fill(width * height)(color)
    image.setRGB(0,0,width,height,pixels,0,width)
    ImageIO.write(image, "JPG", new File(path))
  }
}

object Pixel {
  val BLACK = Pixel(0,0,0)
  val WHITE = Pixel(255,255,255)
  val RED = Pixel(255,0,0)
  val GREEN = Pixel(0,255,0)
  val BLUE = Pixel(0,0,255)
  val GRAY = Pixel(128, 128, 128)

  // clamps a value between 0-255
  def clamp(v: Int): Int =
    if v <= 0 then 0
    else if v >= 255 then 255
    else v
  
  // color = 00000000rrrrrrrrggggggggbbbbbbbb
  // redm =  00000000111111110000000000000000
  def fromHex(color: Int): Pixel =
    Pixel(
      (color & 0xFF0000) >> 16,
      (color & 0xFF00) >> 8,
      color & 0xFF
    )
    
  
  def main(args: Array[String]): Unit = {
    val red = Pixel(255,0,0)
    val green = Pixel(0,255,0)
    val yellow = red + green
    val pink = Transparency(0.5).combine(RED, WHITE)
    val darkRed = Multiply.combine(RED, GRAY)
    val lightRed = Screen.combine(RED, GRAY)
    lightRed.draw(40,40,"photoscala/src/main/resources/pixels/lightred.jpg")
  }
}
