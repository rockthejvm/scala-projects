package com.rockthejvm.guardianscraper

import org.jsoup.Jsoup

import scala.jdk.CollectionConverters.*

case class Headline(title: String, url: String)

object GuardianScraper {

  val url = "https://theguardian.com"
  val contentSelector = "div[id *= container-]>ul>li a"
  val interestingSections = List("world", "football")

  def scrapeHeadlines(section: String): List[Headline] =
    Jsoup.connect(s"$url/$section").get()
      .select(contentSelector)
      .asScala
      .toList
      .map { link =>
        val title = if link.text.isEmpty then link.attr("aria-label") else link.text
        Headline(title, link.attr("href"))
      }

  def main(args: Array[String]): Unit = {
    scrapeHeadlines("world").foreach(println)
  }
}
