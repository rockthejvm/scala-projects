package com.rockthejvm.staticsite

import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import os.{rel => _, *}
import scalatags.Text.all.*

import java.time.{Instant, LocalDate, ZoneOffset}

case class ArticleDetails(slug: String, name: String, date: LocalDate)

object PersonalWebsite {
  val bootstrap =
    link(
      rel := "stylesheet",
      href := "https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css"
    )

  // renders the HTML
  // returns (file name, path, date)
  def renderPost(filePath: Path, outPath: Path) = {
    val s"$filename.md" = filePath.last
    val publishedDate = LocalDate.ofInstant(
      Instant.ofEpochMilli(os.mtime(filePath)),
      ZoneOffset.UTC
    )

    // rendering - parser + renderer
    val parser = Parser.builder().build()
    val parsedAST = parser.parse(os.read(filePath)) // AST
    val renderer = HtmlRenderer.builder().build()
    val htmlOutput = renderer.render(parsedAST)

    // write the contents to a corresponding file
    val pageSlug = s"${filename.toLowerCase.replace(" ", "-")}.html"
    os.write(
      outPath / "article" / pageSlug,
      html(
        head(
          bootstrap
        ),
        body(
          a(href := "../index.html", "<- Back to Blog"),
          raw(htmlOutput),
          p(i(s"Written on $publishedDate"))
        )
      )
    )

    ArticleDetails(pageSlug, filename, publishedDate)
  }

  def renderBlog() = {
    val resourcePath = os.Path("/Users/daniel/dev/rockthejvm/courses/scala-projects/staticsite/src/main/resources")
    val blogRoot = resourcePath / "articles"
    val outPath = resourcePath / "blog_out"

    // clean up the blog out dir
    os.remove.all(outPath)
    os.makeDir.all(outPath / "article")

    // generate the pages
    val articles = os.list(blogRoot)
      .map(filePath => renderPost(filePath, outPath))
      .sortBy(_.date) // most recently modified articles first

    // generate the index.html
    os.write(
      outPath / "index.html",
      html(
        head(
          bootstrap
        ),
        body(
          h1("Rock the JVM blog"),
          articles.map {
            case ArticleDetails(slug, name, publishedDate) =>
              h2(
                a(
                  href := s"article/$slug",
                  name
                )
              )
          }
        )
      )
    )
  }

  def main(args: Array[String]): Unit = {
    renderBlog()
  }
}
