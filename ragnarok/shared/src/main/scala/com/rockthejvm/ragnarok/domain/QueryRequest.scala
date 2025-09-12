package com.rockthejvm.ragnarok.domain

import upickle.default.*

case class QueryRequest (sessionId: String, content: String)
derives ReadWriter
