package com.rockthejvm.photoscala

import java.awt.{Dimension, Graphics}
import javax.swing.{JFrame, JPanel, WindowConstants}
import scala.io.Source

object App {
  private var frame: Option[JFrame] = None
  private var imagePanel: Option[ImagePanel] = None

  class ImagePanel(private var image: Image) extends JPanel {
    override def paintComponent(g: Graphics): Unit = {
      super.paintComponent(g)
      image.draw(g)
    }

    override def getPreferredSize: Dimension =
      new Dimension(image.width, image.height)

    def replaceImage(newImage: Image): Unit = {
      image = newImage
      revalidate()
      repaint()
    }

    def getImage = image
  }

  def loadResource(path: String): Unit = {
    val image = Image.loadResource(path)
    if (frame.isEmpty) {
      // initialization
      val newFrame = new JFrame("PhotoScala")
      val newImagePanel = new ImagePanel(image)

      newFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE)
      newFrame.getContentPane.add(newImagePanel)
      newFrame.pack()
      newFrame.setVisible(true)
      
      frame = Some(newFrame)
      imagePanel = Some(newImagePanel)
    } else {
      imagePanel.foreach(_.replaceImage(image))
      frame.foreach(_.pack())
    }
  }

  // Iterator[String]
  // State(frame, image)
  // update = (State, String) => State

  def main(args: Array[String]): Unit = {
    print("> ")
    Source.stdin.getLines().foreach { command =>
      val words = command.split(" ")
      val action = words(0)
      action match {
        case "load" =>
          try {
            loadResource(words(1))
          } catch {
            case _: Exception =>
              println(s"Error: cannot load image at path ${words(1)}")
          }
        case "save" =>
          if (frame.isEmpty)
            println("Error: no image loaded.")
          else
            imagePanel.foreach(_.getImage.saveResource(words(1)))
        case "exit" =>
          System.exit(0)
        case _ =>
          if (frame.isEmpty)
            println("Error: must have an image loaded before running transformations")
          else imagePanel.foreach { panel =>
            val transformation = Transformation.parse(command)
            val newImage = transformation(panel.getImage)
            panel.replaceImage(newImage)
            frame.foreach(_.pack())
          }
      }

      print("> ")
    }
  }
}
