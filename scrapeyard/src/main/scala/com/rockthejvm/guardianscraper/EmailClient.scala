package com.rockthejvm.guardianscraper

import org.slf4j.LoggerFactory
import pureconfig.ConfigSource

import java.util.{Properties, UUID}
import javax.mail.internet.{InternetAddress, MimeMessage}
import javax.mail.*

class EmailClient private (host: String, port: Int, user: String, password: String) {
  val log = LoggerFactory.getLogger(this.getClass)
  
  def sendEmail(to: String, subject: String, body: String) = {
    // 1 - session
    val props = new Properties()
    props.put("mail.smtp.host", host)
    props.put("mail.smtp.port", port)
    props.put("mail.smtp.auth", true)
    props.put("mail.smtp.starttls.enable", true)
    val session = Session.getInstance(
      props,
      new Authenticator {
        override def getPasswordAuthentication =
          new PasswordAuthentication(user, password)
      }
    )

    // 2 - message
    val msg = new MimeMessage(session)
    msg.setFrom(new InternetAddress("daniel@rockthejvm.com"))
    msg.setRecipients(Message.RecipientType.TO, to)
    msg.setSubject(subject)
    msg.setContent(body, "text/html; charset=utf-8")
    msg.setHeader("Message-ID", UUID.randomUUID().toString)

    // 3 - send the message
    try {
      Transport.send(msg)
      log.info(s"Email sent successfully to $to")
    } catch {
      case e: Exception =>
        e.printStackTrace()
    }
  }
}

object EmailClient {
  val default: EmailClient = ConfigSource.default.load[ScraperConfig]
    .map(_.emailConfig)
    .map {
      case EmailConfig(host, port, user, password) => EmailClient(host, port, user, password)
    }
    .fold(
      e => throw new IllegalArgumentException(s"Config failures for email client. Errors: $e"),
      client => client
    )
}

