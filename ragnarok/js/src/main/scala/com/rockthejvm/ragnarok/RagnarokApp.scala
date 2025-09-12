package com.rockthejvm.ragnarok

import org.scalajs.dom
import org.scalajs.dom.*

import scala.concurrent.ExecutionContext
import scala.scalajs.js
import scala.util.*
import upickle.default.*
import domain.*

import scala.scalajs.js.annotation.JSGlobal

// how to make a JS library available in Scala.js
@js.native
@JSGlobal
object marked extends js.Object {
  def parse(text: String): String = js.native
}

@js.native
@JSGlobal
object DOMPurify extends js.Object {
  def sanitize(dirty: String): String = js.native
}

@js.native
@JSGlobal
object hljs extends js.Object {
  def highlightAll(): Unit = js.native
}

object RagnarokApp extends App {

  private var session: Option[String] = None
  private val backendUrl = "localhost:8080"

  given ExecutionContext = ExecutionContext.Implicits.global

  def setupEventListeners(): Unit = {
    document.getElementById("send-button").addEventListener("click", _ => sendQuestion())
    document.getElementById("question").addEventListener("keypress", (e: KeyboardEvent) => {
      if (e.key == "Enter")
        sendQuestion()
    })
  }

  def sendQuestion(): Unit = {
    val questionElement = document.getElementById("question").asInstanceOf[html.Input]
    val question = questionElement.value

    if (question.trim().nonEmpty) {
      addMessage(question, "user")
      showSpinner()

      val request = new RequestInit {
        method = HttpMethod.POST
        headers = js.Dictionary("Content-Type" -> "application/json")
        body = write(QueryRequest(session.getOrElse(""), question))
      }

      fetch(s"http://$backendUrl/query", request).toFuture
        .foreach { _ =>
          addMessage("", "AI")
          hideSpinner()
        }
    }
  }

  def addMessage(text: String, sender: String): Unit = {
    // fetch the container
    val messageContainer = document.getElementById("messages")
    // create a div = message
    val newDiv = document.createElement("div")
    // add css classes
    val cssClasses = List(
      "message",
      if sender == "user" then "user-message" else "bot-message"
    )
    cssClasses.foreach(cls => newDiv.classList.add(cls))

    if (sender == "user") {
      // if sender = user => render a piece of text
      newDiv.innerHTML = s"<strong>You: $text</strong>"
    } else {
      newDiv.setAttribute("data-raw-content", text)
      // if sender = AI
      //  - parse the text as Markdown - marked
      val parsed = marked.parse(text)
      //  - sanitize the HTML - DOMPurify
      val sanitized = DOMPurify.sanitize(parsed)
      //  - add the final HTML in the new div
      newDiv.innerHTML = s"<strong>AI:</strong> $sanitized"
      //  - add code highlighting for code blocks - highlight.js
      hljs.highlightAll()
    }

    //  - add the div to the container
    messageContainer.appendChild(newDiv)
    messageContainer.scrollTop = messageContainer.scrollHeight
  }

  def processPartial(message: QueryPartialResponse) = {
    val messageContainer = document.getElementById("messages")
    val lastMessage = messageContainer.lastElementChild // assume that the last message has been added

    Option(lastMessage)
      .filter(_.classList.contains("bot-message"))
      .foreach { messageElement =>
        // retrieve the text
        val originalText = Option(messageElement.getAttribute("data-raw-content")).getOrElse("")
        // append the new content
        val newContent = originalText + message.content
        messageElement.setAttribute("data-raw-content", newContent)
        // do the same as addMessage
        val parsed = marked.parse(newContent)
        //  - sanitize the HTML - DOMPurify
        val sanitized = DOMPurify.sanitize(parsed)
        //  - add the final HTML in the new div
        messageElement.innerHTML = s"<strong>AI:</strong> $sanitized"
        //  - add code highlighting for code blocks - highlight.js
        hljs.highlightAll()
        // continue scrolling
        messageContainer.scrollTop = messageContainer.scrollHeight
      }
  }

  def getSession() = {
    val request = new RequestInit {
      method = HttpMethod.GET
      headers = js.Dictionary("Content-Type" -> "application/json")
    }

    fetch(s"http://$backendUrl/session", request).toFuture
      .flatMap(_.text().toFuture)
      .map(read[String](_))
      .onComplete {
        case Success(sessionId) =>
          dom.console.log(s"Session fetched: $sessionId")
          session = Some(sessionId)
          connectWebSocket(sessionId)
        case Failure(e) =>
          dom.console.log(s"Error fetching session: $e")
      }
  }

  // ws /subscribe/:sessionId
  def connectWebSocket(sessionId: String) = {
    val webSocket = new WebSocket(s"ws://$backendUrl/subscribe/$sessionId")

    webSocket.onmessage = { (e: MessageEvent) =>
      val data = e.data.toString
      val qpr = read[QueryPartialResponse](data)
      processPartial(qpr)
    }
    window.onbeforeunload = _ => webSocket.close()
  }

  def showSpinner(): Unit = {
    document.getElementById("spinner").asInstanceOf[html.Div].style.display = "flex"
  }

  def hideSpinner(): Unit = {
    document.getElementById("spinner").asInstanceOf[html.Div].style.display = "none"
  }

  getSession()
  setupEventListeners()
}
