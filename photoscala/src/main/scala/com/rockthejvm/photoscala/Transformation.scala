package com.rockthejvm.photoscala

import java.io.IOException
import scala.annotation.targetName

trait Transformation {
  def apply(image: Image): Image
}

object Transformation {
  def parse(string: String): Transformation = {
    val words = string.split(" ")
    val command = words(0)
    command match {
      case "crop" =>
        try {
          Crop(
            words(1).toInt,
            words(2).toInt,
            words(3).toInt,
            words(4).toInt
          )
        } catch {
          case _: Exception =>
            println("Invalid crop command. Usage: 'crop [x] [y] [w] [h]'")
            Noop
        }
      // per-pixel transformations
      case "invert" => Invert
      case "grayscale" => Grayscale
      case "colorize" =>
        try {
          val colorHex = words(1)
          val colorCode = Integer.parseInt(colorHex, 16)
          Colorize(Pixel.fromHex(colorCode))
        } catch {
          case _: Exception =>
            println("Invalid colorize command. Usage: 'colorize [hexcode]'")
            Noop
        }
      // kernel filters
      case "blur" => KernelFilter(Kernel.blur)
      case "sharpen" => KernelFilter(Kernel.sharpen)
      case "edge" => KernelFilter(Kernel.edge)
      case "emboss" => KernelFilter(Kernel.emboss)
      // blending
      case "blend" => 
        try {
          Blend(
            Image.loadResource(words(1)),
            BlendMode.parse(words(2))
          )
        } catch {
          case _: IOException =>
            println(s"Cannot load image at ${words(1)}")
            Noop
          case _: Exception =>
            println("Invalid blend format. Usage: 'blend [path] [mode]'")
            Noop
        }
      
      // not supported
      case _ =>
        println(s"Invalid command: '$command'")
        Noop
    }
  }
}

case object Noop extends Transformation {
  override def apply(image: Image) = image
}

case class Crop(x: Int, y: Int, w: Int, h: Int) extends Transformation {
  override def apply(image: Image) =
    try {
      image.crop(x,y,w,h)
    } catch {
      case _: Exception =>
        println(s"Error: coordinates are out of bounds. Max coordinates: ${image.width} ${image.height}")
        image
    }
}

case object Grayscale extends Transformation {
  override def apply(image: Image) =
    image.map { pixel =>
      val avg = (pixel.r + pixel.g + pixel.b) / 3
      Pixel(avg, avg, avg)
    }
}

case object Invert extends Transformation {
  override def apply(image: Image) =
    image.map { pixel =>
      Pixel(
        255 - pixel.r,
        255 - pixel.g,
        255 - pixel.b,
      )
    }
}

case class Colorize(color: Pixel) extends Transformation {
  override def apply(image: Image) =
    image.map { pixel =>
      val avg = (pixel.r + pixel.g + pixel.b) / 3
      Pixel(
        (color.r * (avg / 255.0)).toInt,
        (color.g * (avg / 255.0)).toInt,
        (color.b * (avg / 255.0)).toInt,
      )
    }
}

// brightness, contrast
// hue, saturation, lightness
// levels, curves

case class Blend(fgImage: Image, blendMode: BlendMode) extends Transformation {
  override def apply(bgImage: Image) = {
    if (fgImage.width != bgImage.width || fgImage.height != bgImage.height) {
      println(s"Error: images don't have the same sizes: ${fgImage.width} x ${fgImage.height} vs  ${bgImage.width} x ${bgImage.height}")
      bgImage
    } else {
      val width = fgImage.width
      val height = fgImage.height

      val result = Image.black(width, height)

      for {
        x <- 0 until width
        y <- 0 until height
      } do result.setColor(
        x,
        y,
        blendMode.combine(
          fgImage.getColor(x,y),
          bgImage.getColor(x,y)
        )
      )

      result
    }
  }
}

case class Window(width: Int, height: Int, pixels: List[Pixel])
case class Kernel(width: Int, height: Int, values: List[Double]) {
  // all the values must sum up to 1.0
  def normalize(): Kernel = {
    val sum = values.sum
    if sum == 0 then this
    else Kernel(width, height, values.map(_ / sum))
  }

  def multiply_v2(window: Window): Pixel = {
    assert(this.width == window.width && this.height == window.height)

    val (red, green, blue) = window.pixels
      .zip(values)
      .map {
        case (Pixel(r,g,b), k) => (r * k, g * k, b * k)
      }
      .reduce {
        case ((r1,g1,b1), (r2,g2,b2)) => (r1 + r2, g1 + g2, b1 + b2)
      }
    
    Pixel(red.toInt, green.toInt, blue.toInt)
  }

  @targetName("multiply")
  infix def *(window: Window): Pixel = {
    assert(this.width == window.width && this.height == window.height)

    val red = window.pixels
      .map(_.r)
      .zip(values)
      .map { case (v, k) => v * k }
      .sum
      .toInt

    val green = window.pixels
      .map(_.g)
      .zip(values)
      .map { case (v, k) => v * k }
      .sum
      .toInt

    val blue = window.pixels
      .map(_.b)
      .zip(values)
      .map { case (v, k) => v * k }
      .sum
      .toInt

    Pixel(red, green, blue)
  }
}
object Kernel {
  val blur = Kernel(3,3,List(
    1.0, 2.0, 1.0,
    2.0, 4.0, 2.0,
    1.0, 2.0, 1.0
  )).normalize()

  val sharpen = Kernel(3,3,List(
    0.0, -1.0, 0.0,
    -1.0, 5.0, -1.0,
    0.0, -1.0, 0.0
  )).normalize()

  val edge = Kernel(3,3, List(
    1.0, 0.0, -1.0,
    2.0, 0.0, -2.0,
    1.0, 0.0, -1.0
  ))

  val emboss = Kernel(3,3, List(
    -2.0, -1.0, 0.0,
    -1.0, 1.0, 1.0,
    0.0, 1.0, 2.0
  ))
}

case class KernelFilter(kernel: Kernel) extends Transformation {
  override def apply(image: Image) = {
    val pixels = for {
      y <- 0 until image.height
      x <- 0 until image.width
    } yield kernel * image.window(x, y, kernel.width, kernel.height)

    Image(image.width, image.height, pixels.toArray)
  }
}
