package com.rockthejvm.ragnarok

import cask.*
import com.rockthejvm.ragnarok.domain.QueryPartialResponse
import dev.langchain4j.model.chat.response.{ChatResponse, StreamingChatResponseHandler}
import upickle.default.*

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

object RagnarokServer extends MainRoutes {
  private val wsConnections = new ConcurrentHashMap[String, WsChannelActor]()
  private val assistants = new ConcurrentHashMap[String, Assistant]()

//  private val assistant = Assistant()
  

  // get /session -> UUID
  @get("/session")
  def getSession() =
    write(UUID.randomUUID().toString)

  // ws /subscribe/:sessionId -> WebSocket
  @websocket("/subscribe/:sessionId")
  def subscribe(sessionId: String): WsHandler =
    WsHandler { connection =>
      // remember the websocket for this session
      wsConnections.put(sessionId, connection)
      println(s"building assistant for $sessionId")
      assistants.put(sessionId, Assistant())
      println(s"getting assistant for $sessionId: ${assistants.get(sessionId)}")

      WsActor {
        case Ws.Close(_, _) =>
          wsConnections.remove(sessionId)
          assistants.remove(sessionId)
      }
    }

  // post /query { sessionId, content } -> 200
  @postJson("/query")
  def postQuery(sessionId: String, content: String) =
    Option(wsConnections.get(sessionId)).map { connection =>
      // happy path
      val assistant = assistants.get(sessionId)
      println(s"getting assistant for $sessionId: $assistant")
      val relevantArticles = Assistant.getReferences(content) // list of articles

      assistant.reply(content)
        .onPartialResponse { token =>
          connection.send(Ws.Text(write(QueryPartialResponse(token))))
        }
        .onCompleteResponse { _ =>
          val linksFooter =
            relevantArticles
              .map(slug => s"- https://rockthejvm.com/article/$slug")
              .mkString("\n", "\n", "\n")

          connection.send(Ws.Text(write(
            s"""
               |
               |Links:
               |$linksFooter
               |""".stripMargin
          )))
        }
        .onError(_.printStackTrace())
        .start()

      write("OK")
    }.getOrElse(write(s"Not ok: session $sessionId cannot be found"))

  // /static -> static HTML + JS + CSS
  @staticFiles("/static")
  def serveStatic() =
    "/Users/daniel/dev/rockthejvm/courses/scala-projects/ragnarok/js/static"

  Assistant.startIngestion()
  initialize()
}
