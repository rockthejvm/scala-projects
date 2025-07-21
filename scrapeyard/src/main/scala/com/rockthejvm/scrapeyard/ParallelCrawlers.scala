package com.rockthejvm.scrapeyard

import java.util.concurrent.Executors
import scala.annotation.tailrec
import scala.concurrent.{Await, ExecutionContext, Future}
import scala.concurrent.duration.*

object ParallelCrawlers {
  def fetchLinks(title: String): List[String] = {
    @tailrec
    def fetchLinksRec(continue: Option[Map[String, String]], acc: List[String], stopLength: Int = 50): List[String] =
      if acc.length > stopLength then acc
      else continue match {
        case None => acc
        case Some(continueFlags) =>
          val response = requests.get(
            "http://en.wikipedia.org/w/api.php",
            params = Map(
              "action" -> "query",
              "titles" -> title,
              "prop" -> "links",
              "format" -> "json",
            ) ++ continueFlags
          )

          val newLinks = for {
            page <- ujson.read(response)("query")("pages").obj.values.toList
            links <- page.obj.get("links").toList.filter(!_.isNull)
            link <- links.arr
          } yield link("title").str

          val newContinue =
            ujson.read(response).obj.get("continue").map(_.obj.view.mapValues(_.str).toMap)

          fetchLinksRec(newContinue, acc ++ newLinks)
      }

    fetchLinksRec(Some(Map()), List())
  }

  def fetchAllLinksPar(startTitle: String, depth: Int = 2)(using ExecutionContext): List[String] = {
    def bfs(level: Int = 0, seen: Set[String] = Set(), current: List[String] = List()): List[String] =
      if level >= depth then seen.toList
      else {
        val futures = current.map(title => Future(fetchLinks(title)))
        val bigFuture = Future.sequence(futures).map(_.flatten)
        val nextTitles = Await.result(bigFuture, 1.minute)
        bfs(level + 1, seen ++ nextTitles, nextTitles)
      }

    bfs(0, Set(startTitle), List(startTitle))
  }

  def main(args: Array[String]): Unit = {
    given ExecutionContext = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(8))
    fetchAllLinksPar("Bucharest").foreach(println)
  }
}
