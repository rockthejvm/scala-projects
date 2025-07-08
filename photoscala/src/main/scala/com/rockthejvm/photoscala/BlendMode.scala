package com.rockthejvm.photoscala

trait BlendMode {
  def combine(fg: Pixel, bg: Pixel): Pixel
}

object BlendMode {
  def parse(word: String): BlendMode = word match {
    case "multiply" => Multiply
    case "screen" => Screen
    case "overlay" => Overlay
    case "transparency" => Transparency(0.5)
    case _ =>
      println("Invalid blend mode. Running no-blend.")
      NoBlend
  }
}

case object NoBlend extends BlendMode {
  override def combine(fg: Pixel, bg: Pixel) = fg
}

class Transparency(f: Double) extends BlendMode {
  private val factor =
    if f <= 0 then 0.0
    else if f >= 1.0 then 1.0
    else f

  // result = fg * f + bg * (1-f)
  override def combine(fg: Pixel, bg: Pixel) =
    Pixel(
      (fg.r * factor + bg.r * (1-factor)).toInt,
      (fg.g * factor + bg.g * (1-factor)).toInt,
      (fg.b * factor + bg.b * (1-factor)).toInt,
    )
}

object Multiply extends BlendMode {
  // result = (fg/255 * bg/255) * 255
  override def combine(fg: Pixel, bg: Pixel) =
    Pixel(
      (fg.r * bg.r / 255.0).toInt,
      (fg.g * bg.g / 255.0).toInt,
      (fg.b * bg.b / 255.0).toInt,
    )
}

object Screen extends BlendMode {
  // result = 255 - ((255 - fg)/255 * (255 - bg)/255) * 255
  override def combine(fg: Pixel, bg: Pixel) =
    Pixel(
      (255 - (255 - fg.r) * (255 - bg.r) / 255.0).toInt,
      (255 - (255 - fg.g) * (255 - bg.g) / 255.0).toInt,
      (255 - (255 - fg.b) * (255 - bg.b) / 255.0).toInt,
    )
}

object Overlay extends BlendMode {
  private def f(a: Double, b: Double): Double =
    if a < 0.5 then 2 * a * b
    else 1 - 2 * (1-a) * (1-b)

  override def combine(fg: Pixel, bg: Pixel) =
    Pixel(
      (255 * f(bg.r / 255.0, fg.r / 255.0)).toInt,
      (255 * f(bg.g / 255.0, fg.g / 255.0)).toInt,
      (255 * f(bg.b / 255.0, fg.b / 255.0)).toInt
    )
}