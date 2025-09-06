package com.rockthejvm.chat.domain

import upickle.default.*

case class Message (
  id: String,
  sender: String,
  content: String,
  parentId: Option[String],
  timestamp: Long // unix time
) derives ReadWriter
