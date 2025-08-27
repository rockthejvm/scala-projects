package com.rockthejvm.guardianscraper

import org.quartz.{Job, JobExecutionContext}
import org.slf4j.LoggerFactory

import java.time.LocalDateTime

class NewsService extends Job {
  override def execute(context: JobExecutionContext): Unit = {
    // TODO fetch all subscribers from some persistent store
    execute(
      List("user1@email.com", "user2@rockthejvm.com")
    )
  }

  def execute(subscribers: List[String]): Unit = {
    val headlinesSections = GuardianScraper.interestingSections.map { section =>
      val headlines = GuardianScraper.scrapeHeadlines(section)
      val sectionTags = headlines.map(h => s"<li><a href=\"${h.url}\">${h.title}</a></li>")
      sectionTags.mkString(s"<h2>$section</h2><div><ul>","","</ul></div>")
    }

    val emailContents =
      s"""
         |<h1>Rock the JVM Guardian Scraper - news</h1>
         |<div>
         |${headlinesSections.mkString("<br/><br/>")}
         |</div>
         |""".stripMargin

    subscribers.foreach { address =>
      EmailClient.default.sendEmail(address, s"Rock the JVM Guardian scraper - ${LocalDateTime.now()}", emailContents)
    }
  }
}
