package com.rockthejvm.chat

import scalasql.*
import scalasql.simple.{*, given}
import scalasql.dialects.PostgresDialect.*

import java.sql.DriverManager

import domain.*

object ChatDb {
  object Messages extends SimpleTable[Message]

  val dbClient = new DbClient.Connection(
    DriverManager.getConnection("jdbc:postgresql://localhost:5432/", "docker", "docker"),
  )
  val db = dbClient.getAutoCommitClientConnection
  
  def getAllMessages(): List[Message] =
    db.run(Messages.select).toList
    
  def getMessagesByUser(user: String): List[Message] =
    db.run(Messages.select.filter(_.sender === user)).toList
    
  def saveMessage(message: Message): Unit =
    db.run(
      Messages.insert.columns( // insert into messages (id, sender, content ...) values (? ? ? ? ?)
        _.id := message.id,
        _.sender := message.sender,
        _.content := message.content,
        _.parentId := message.parentId,
        _.timestamp := message.timestamp
      )
    )
  
}
