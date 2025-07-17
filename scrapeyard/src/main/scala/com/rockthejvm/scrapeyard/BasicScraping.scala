package com.rockthejvm.scrapeyard

import org.jsoup.Jsoup
import org.jsoup.nodes.Element

import scala.jdk.CollectionConverters.*

object BasicScraping {

  def getWikipedia() = {
    val doc = Jsoup.connect("https://en.wikipedia.org").get()
    println(doc.title)
    val headlines = doc.select("#mp-itn b a").asScala
    headlines.map(_.attr("title")).foreach(println)
  }

  def getWebAPIsFromMDN() = {
    val mainDoc = Jsoup.connect("https://developer.mozilla.org/en-US/docs/Web/API").get()
    val links = mainDoc.select("h2#interfaces").next.select("div.index").select("a").asScala
    val linkData = links.map { link =>
      (link.attr("href"), link.attr("title"))
    }

    val articles = linkData.take(5).map {
      case (url, title) =>
        println(s"Scraping $title")
        val doc = Jsoup.connect(s"https://developer.mozilla.org$url").get()
        val summary = doc.select("article.main-page-content p")
          .asScala
          .headOption
          .map(_.text)
          .getOrElse("(no description)")

        val methodsAndProperties = doc.select("article.main-page-content dl dt")
          .asScala
          .map { term =>
            val name = term.text
            val description = Option(term.nextElementSibling)
              .map(_.text)
              .getOrElse("(no description)")

            s"$name - $description"
          }


        s"""
           |$title - $summary
           |\t${methodsAndProperties.mkString("\n\t")}
           |""".stripMargin
    }

    articles.foreach(println)
  }

  case class Comment(author: String, content: String, replies: List[Comment])

  def parseLobstersDiscussion(url: String): List[Comment] = {
    def recurse(node: Element): Comment = {
      val user = node.select("div.byline a")
        .asScala
        .find(link => Option(link.attr("href")).exists(_.contains("~"))) // finds the user handle link
        .map(link => link.attr("href").substring(1)) // drop the /
        .getOrElse("(anonymous)") // should never happen

      val content = node.selectFirst("div.comment_text").text
      val replies = node.selectFirst("ol.comments").children().asScala.map(recurse).toList
      Comment(user, content, replies)
    }

    val doc = Jsoup.connect("https://lobste.rs" + url).get()

    doc.select("ol.comments.comments1 ol.comments > li.comments_subtree")
      .asScala
      .map(recurse)
      .toList
  }

  def main(args: Array[String]): Unit = {
    parseLobstersDiscussion("/s/ugyxdj/new_date_wtf").foreach(println)
  }
}
