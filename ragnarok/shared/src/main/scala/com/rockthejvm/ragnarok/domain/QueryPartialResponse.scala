package com.rockthejvm.ragnarok.domain

import upickle.default.*

case class QueryPartialResponse (content: String) derives ReadWriter
