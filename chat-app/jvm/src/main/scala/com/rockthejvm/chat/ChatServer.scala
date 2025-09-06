package com.rockthejvm.chat

import cask.*
import upickle.default.*

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import scala.collection.mutable.ArrayBuffer

import domain.*

object ChatServer extends MainRoutes {
  private val SYSTEM = "SYSTEM"
  private val db = ChatDb
  private val wsConnections = ConcurrentHashMap.newKeySet[WsChannelActor]()

  // GET /messages => List[Message]
  @getJson("/messages")
  def retrieveAllMessages() =
    db.getAllMessages()

  // GET /messages/search/:user => List[Message] sent by that user
  @getJson("/messages/search/:user")
  def queryMessages(user: String) =
    db.getMessagesByUser(user)

  // POST /chat { sender, content, parent, timestamp } => the message that has just been sent
  @postJson("/chat")
  def sendMessage(sender: String, content: String, parentId: Option[String] = None, timestamp: Long = -1L) =
    if (sender.trim.isEmpty) writeJs(ErrorMessage("Sender cannot be empty"))
    else if (content.trim.isEmpty) writeJs(ErrorMessage("Content cannot be empty"))
    else {
      val newMessage = Message(
        id = UUID.randomUUID().toString,
        sender = sender,
        content = content,
        parentId = parentId,
        timestamp = if (timestamp > 0) timestamp else System.currentTimeMillis()
      )

      // save the message
      db.saveMessage(newMessage)

      // broadcast the message to everyone
      wsConnections.forEach(_.send(Ws.Text(write(newMessage))))

      // return the message
      writeJs(newMessage)
    }

  /*
    WS /subscribe/:user
      - receive all messages
      - broadcast a "system" message "user has entered the chat"
      - receive all messages from all other users
      - send a message => broadcast ^^
   */
  @websocket("/subscribe/:user")
  def subscribe(user: String) = WsHandler { channel =>
    // retrieve all messages
    db.getAllMessages().foreach { message =>
      channel.send(Ws.Text(write(message)))
    }
    // save the connection
    wsConnections.add(channel)

    // broadcast "$user has joined the chat" to everyone
    wsConnections.forEach(_.send(Ws.Text(write(
      Message(
        id = UUID.randomUUID().toString,
        sender = SYSTEM,
        content = s"$user joined the chat",
        parentId = None,
        timestamp = System.currentTimeMillis()
      )
    ))))

    // return an actor that will
    //  - on new message => broadcast it to everyone
    //  - on closure => remove the connection + broadcast "$user left the chat" to everyone
    WsActor {
      case event @ Ws.Text(text) =>
        val msg = read[Message](text)
        // check the sender
        if (msg.sender != user) {
          channel.send(Ws.Text(write(
            Message(
              id = UUID.randomUUID().toString,
              sender = SYSTEM,
              content = s"Error: somehow the sender is not the same as the user being subscribed.",
              parentId = None,
              timestamp = System.currentTimeMillis()
            )
          )))
        } else {
          // set the id of the message in case the id is empty
          if (msg.id.isEmpty)
            db.saveMessage(msg.copy(id = UUID.randomUUID().toString))
          else 
            db.saveMessage(msg)
          wsConnections.forEach(_.send(event))
        }
      case Ws.Close(_, _) =>
        wsConnections.remove(channel)
        wsConnections.forEach(_.send(Ws.Text(write(
          Message(
            id = UUID.randomUUID().toString,
            sender = SYSTEM,
            content = s"$user left the chat",
            parentId = None,
            timestamp = System.currentTimeMillis()
          )
        ))))
    }
  }

  // GET /static => frontend
  @staticFiles("/static")
  def serveStaticFiles() =
    "/Users/daniel/dev/rockthejvm/courses/scala-projects/chat-app/js/static"

  initialize()
}
