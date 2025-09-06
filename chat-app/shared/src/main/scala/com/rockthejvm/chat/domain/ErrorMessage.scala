package com.rockthejvm.chat.domain

import upickle.default.*

case class ErrorMessage (content: String) derives ReadWriter
