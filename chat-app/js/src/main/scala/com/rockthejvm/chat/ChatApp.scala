package com.rockthejvm.chat

import org.scalajs.dom
import org.scalajs.dom.{MessageEvent, WebSocket, document, html}

import scala.scalajs.js
import upickle.default.*
import scalatags.JsDom.all.*
import domain.*

import scala.util.Random

object ChatApp extends App {

  private val nameInput = document.querySelector("header input").asInstanceOf[html.Input]
  private val chatContainer = document.querySelector(".chat-container").asInstanceOf[html.Div]
  private val chatInput = document.querySelector("footer input").asInstanceOf[html.Input]

  private val colors = List(
    "#2196F3", "#F44336", "#4CAF50", "#9C27B0", "#FF9800", "#00BCD4",
    "#673AB7", "#FF5722", "#3F51B5", "#8BC34A", "#009688", "#E91E63",
    "#607D8B", "#795548", "#6200EA", "#00897B", "#C0392B", "#303F9F",
    "#D32F2F", "#0288D1", "#388E3C", "#512DA8", "#FFA000"
  )

  // mutable variables
  private var username: Option[String] = None
  private var ws: Option[WebSocket] = None
  private var replyTo: Option[String] = None // the id of the message I'm about to reply to
  private val userColors = collection.mutable.Map[String, String]()

  private def wsUrl(name: String) = s"ws://localhost:8080/subscribe/$name"
  private def assignColor(user: String) =
    userColors.getOrElseUpdate(
      user,
      colors(Random.nextInt(colors.length))
    )

  def connectWebSocket(name: String) = {
    val newws = new WebSocket(wsUrl(name))

    newws.onmessage = { (e: MessageEvent) =>
      val data = e.data.toString
      val message = read[Message](data)
      renderMessage(message)
    }

    // save the ws
    ws = Some(newws)

    // close the ws once the user leaves the page
    dom.window.onbeforeunload = _ => newws.close()
  }

  def renderSystemMessage(message: Message) =
    chatContainer.appendChild(
      div(
        cls := "system-message",
        message.content
      ).render
    )

  def renderUserMessage(message: Message): Unit = {
    val date = new js.Date(message.timestamp)
    val timeString = date.toLocaleTimeString()

    val baseCssClasses =
      if (message.sender == username.getOrElse("")) List("message", "self")
      else List("message", "other")

    val messageDiv =
      div(
        cls := baseCssClasses.mkString(" "),
        attr("data-id") := message.id, // allows me to fetch the id of the message from the UI
        message.parentId
          .flatMap(pid => Option(chatContainer.querySelector(s"div.message[data-id='$pid']")))
          .map { parentEl => // the chat bubble with the parent message
            val parentSender = parentEl.querySelector("b").textContent
            val parentContent = parentEl.querySelector("div:nth-of-type(2)").textContent
            div(
              cls := "reply-preview",
              small(b(parentSender), s": $parentContent")
            )
          }, 
        // sender
        div(b(
          cls := "message-sender",
          message.sender
        )),
        // content
        div(
          cls := "message-content",
          message.content
        ),
        // meta = timestamp + reply link
        div(
          cls := "meta",
          timeString + " ", // child
          span(
            cls := "reply-link",
            onclick := { (e: dom.Event) =>
              val target = e.target.asInstanceOf[dom.Element]
              val messageEl = target.closest(".message").asInstanceOf[html.Div]
              replyTo = Option(messageEl.getAttribute("data-id"))

              val sender = messageEl.querySelector("b.message-sender").textContent
              val content = messageEl.querySelector("div.message-content").textContent
              val previewText = s"$sender: $content"
              dom.console.log(s"attempting to reply to '$previewText'")

              var preview = document.querySelector(".input-reply-preview")
              if (preview == null) {
                preview = div(
                  cls := "input-reply-preview",
                ).render
                chatInput.parentNode.insertBefore(preview, chatInput)
              }

              preview.textContent = previewText
            },
            "Reply"
          )
        )
      ).render

    if (message.sender != username.getOrElse(""))
      messageDiv.style.background = assignColor(message.sender)

    chatContainer.appendChild(messageDiv)
    chatContainer.scrollTop = chatContainer.scrollHeight // scroll all the way down
  }

  def renderMessage(message: Message): Unit =
    if (message.sender == "SYSTEM") renderSystemMessage(message)
    else renderUserMessage(message)

  def setupEvents(): Unit = {
    nameInput.onkeydown = { (e: dom.KeyboardEvent) =>
      if (e.key == "Enter" && nameInput.value.trim.nonEmpty && username.isEmpty) {
        val name = nameInput.value.trim
        username = Some(name)
        nameInput.disabled = true
        dom.console.log(s"Logged in as $name")
        connectWebSocket(name)
      }
    }

    chatInput.onkeydown = { (e: dom.KeyboardEvent) =>
      if (e.key == "Enter" && chatInput.value.trim.nonEmpty && username.nonEmpty) {
        val message = Message(
          id = "", // server will create one
          sender = username.get, // can call .get
          content = chatInput.value.trim,
          parentId = replyTo,
          timestamp = js.Date.now().toLong
        )

        ws.foreach(_.send(write(message)))
        chatInput.value = "" // reset the input

        // resets the reply preview
        val preview = document.querySelector(".input-reply-preview")
        if (preview != null) preview.parentNode.removeChild(preview)
        replyTo = None
      }
    }
  }

  setupEvents()
}
