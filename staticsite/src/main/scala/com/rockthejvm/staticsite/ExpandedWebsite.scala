package com.rockthejvm.staticsite

import org.commonmark.parser.Parser
import org.commonmark.renderer.html.HtmlRenderer
import os.{rel => _, *}
import scalatags.Text.all.*
import scalatags.Text.tags2

import java.time.{Instant, LocalDate, ZoneOffset}
import java.time.format.DateTimeFormatter

case class ArticleMeta(
                           slug: String,
                           name: String,
                           date: LocalDate,
                           category: Option[String] = None,
                           description: Option[String] = None
                         )

object ExpandedWebsite {
  // Paths configuration
  val resourcePath = os.Path("/Users/daniel/dev/rockthejvm/courses/scala-projects/staticsite/src/main/resources")
  val blogRoot = resourcePath / "articles"
  val outPath = resourcePath / "blog_expanded_out"
  val staticPagesRoot = resourcePath / "pages"

  // Site configuration
  val siteName = "Rock the JVM Blog"
  val siteAuthor = "Daniel Ciocîrlan"
  val siteDescription = "Exploring Scala, functional programming, and the JVM ecosystem"

  // Styling and assets
  val customCss = link(rel := "stylesheet", href := "/css/styles.css")
  val bootstrapCss = link(rel := "stylesheet", href := "https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/css/bootstrap.min.css")
  val fontAwesome = link(rel := "stylesheet", href := "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.4.0/css/all.min.css")
//  val hljsBasicCss = link(rel := "stylesheet", href :="https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/styles/default.min.css")
  val hljsAtomCss = link(rel := "stylesheet", href := "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.0.1/styles/atom-one-dark.min.css")
  val hljs = script(src := "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/highlight.min.js")
  val hljsScala = script(src := "https://cdnjs.cloudflare.com/ajax/libs/highlight.js/11.11.1/languages/scala.min.js")
  val hljsTrigger = script("hljs.highlightAll()")
  val bootstrapJs = script(src := "https://cdn.jsdelivr.net/npm/bootstrap@5.3.8/dist/js/bootstrap.bundle.min.js")
  // Common page elements
  def pageHeader = header(cls := "site-header container-fluid py-3",
    div(cls := "row align-items-center",
      div(cls := "col-md-4 d-flex",
        h1(cls := "site-title", a(href := "/index.html", siteName))
      ),
      div(cls := "col-md-8",
        tags2.nav(cls := "site-nav",
          ul(cls := "nav justify-content-end",
            li(cls := "nav-item", a(cls := "nav-link", href := "/index.html", "Blog")),
            li(cls := "nav-item", a(cls := "nav-link", href := "/about.html", "About")),
            li(cls := "nav-item", a(cls := "nav-link", href := "/projects.html", "Projects")),
            li(cls := "nav-item", a(cls := "nav-link", href := "/contact.html", "Contact"))
          )
        )
      )
    )
  )

  def pageFooter = footer(cls := "site-footer mt-5 py-4",
    div(cls := "container",
      div(cls := "row",
        div(cls := "col-md-4",
          h5("About"),
          p(siteDescription),
          p(s"© ${LocalDate.now().getYear} $siteAuthor")
        ),
        div(cls := "col-md-4",
          h5("Links"),
          ul(cls := "list-unstyled",
            li(a(href := "/index.html", "Blog")),
            li(a(href := "/about.html", "About")),
            li(a(href := "/projects.html", "Projects")),
            li(a(href := "/contact.html", "Contact"))
          )
        ),
        div(cls := "col-md-4",
          h5("Connect"),
          div(cls := "social-links",
            a(href := "https://github.com/rockthejvm", cls := "me-2", i(cls := "fab fa-github")),
            a(href := "https://twitter.com/rockthejvm", cls := "me-2", i(cls := "fab fa-twitter")),
            a(href := "https://www.youtube.com/rockthejvm", cls := "me-2", i(cls := "fab fa-youtube"))
          )
        )
      )
    )
  )

  // Common layout
  def pageLayout(pageTitle: String, contents: Modifier*) = html(
    head(
      meta(charset := "UTF-8"),
      meta(name := "viewport", content := "width=device-width, initial-scale=1.0"),
      meta(name := "description", content := siteDescription),
      title := s"$pageTitle | $siteName",
      bootstrapCss,
      fontAwesome,
      customCss,
      hljsAtomCss
    ),
    body(
      pageHeader,
      tags2.main(
        cls := "container py-4",
        contents
      ),
      pageFooter,
      bootstrapJs,
      hljs,
      hljsScala,
      hljsTrigger
    )
  )

  // Extract metadata from article content
  def extractMetadata(content: String): Map[String, String] = {
    /*
     ---
     title: First post
     date: ...
     ---

     content
     */
    val metadataPattern = """^---\s*\n([\s\S]*?)\n---\s*\n""".r
    metadataPattern.findFirstMatchIn(content).map { m =>
      val metadataBlock = m.group(1)
      metadataBlock.split("\n").map { line =>
        val parts = line.split(":", 2)
        if (parts.length == 2) parts(0).trim -> parts(1).trim
        else "" -> ""
      }.filter(_._1.nonEmpty).toMap
    }.getOrElse(Map.empty)
  }

  // cuts down the frontmatter
  def contentWithoutMetadata(content: String): String = {
    val metadataPattern = """^---\s*\n[\s\S]*?\n---\s*\n""".r
    metadataPattern.replaceFirstIn(content, "")
  }

  // Rendering functions
  def renderPost(filePath: Path, outPath: Path): ArticleMeta = {
    val s"$filename.md" = filePath.last
    val fileContent = os.read(filePath)
    val metadata = extractMetadata(fileContent)
    val content = contentWithoutMetadata(fileContent)

    val parser = Parser.builder().build()
    val parsedAST = parser.parse(content)
    val renderer = HtmlRenderer.builder().build()
    val htmlOutput = renderer.render(parsedAST)

    val publishedDate = metadata.get("date")
      .map(date => LocalDate.parse(date))
      .getOrElse(LocalDate.ofInstant(Instant.ofEpochMilli(os.mtime(filePath)), ZoneOffset.UTC))

    val title = metadata.getOrElse("title", filename)
    val category = metadata.get("category")
    val description = metadata.get("description")

    val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy")
    val formattedDate = publishedDate.format(dateFormatter)

    val pageSlug = s"${filename.toLowerCase.replace(" ", "-")}.html"

    os.write(
      outPath / "article" / pageSlug,
      pageLayout(
        title,
        div(cls := "article-header mb-4",
          h1(cls := "article-title", title),
          div(cls := "article-meta",
            span(cls := "article-date me-3",
              i(cls := "far fa-calendar-alt me-1"), formattedDate
            ),
            category.map(cat =>
              span(cls := "article-category",
                i(cls := "fas fa-folder me-1"), cat
              )
            )
          )
        ),
        div(cls := "article-content",
          raw(htmlOutput)
        ),
        hr(),
        div(cls := "article-footer mt-4",
          a(href := "../index.html", cls := "btn btn-outline-primary",
            i(cls := "fas fa-arrow-left me-1"), "Back to Blog"
          )
        )
      ).toString
    )

    ArticleMeta(pageSlug, title, publishedDate, category, description)
  }

  def renderStaticPage(file: Path, outPath: Path): Unit = {
    val s"$filename.md" = file.last
    val fileContent = os.read(file)
    val metadata = extractMetadata(fileContent)
    val content = contentWithoutMetadata(fileContent)
    val parser = Parser.builder().build()
    val parsedAST = parser.parse(content)
    val renderer = HtmlRenderer.builder().build()
    val htmlOutput = renderer.render(parsedAST)

    val title = metadata.getOrElse("title", filename.capitalize)

    os.write(
      outPath / s"${filename.toLowerCase}.html",
      pageLayout(
        title,
        div(cls := "page-header mb-4",
          h1(title)
        ),
        div(cls := "page-content",
          raw(htmlOutput)
        )
      ).toString
    )
  }

  def renderBlogIndex(articles: Seq[ArticleMeta]): Unit = {
    val articlesByYear = articles
      .sortBy(_.date)(Ordering[LocalDate].reverse)
      .groupBy(_.date.getYear)
      .toSeq
      .sortBy(_._1)(Ordering[Int].reverse)

    os.write(
      outPath / "index.html",
      pageLayout(
        "Blog",
        div(cls := "blog-header mb-4",
          h1("Blog"),
          p(cls := "lead", "Thoughts, tutorials, and insights about programming")
        ),
        div(cls := "row",
          div(cls := "col-md-8",
            articlesByYear.map { case (year, yearArticles) =>
              frag(
                h2(cls := "year-heading mt-4", year.toString),
                div(cls := "article-list",
                  yearArticles.map { article =>
                    val dateFormatter = DateTimeFormatter.ofPattern("MMM d")
                    val shortDate = article.date.format(dateFormatter)

                    div(cls := "article-item mb-4",
                      div(cls := "d-flex",
                        div(cls := "article-date me-3",
                          span(cls := "badge bg-light text-dark", shortDate)
                        ),
                        div(cls := "article-info",
                          h3(cls := "article-title mb-1",
                            a(href := s"article/${article.slug}", article.name)
                          ),
                          article.description.map(desc => p(cls := "article-description text-muted", desc)),
                          article.category.map(cat =>
                            span(cls := "badge bg-secondary", cat)
                          )
                        )
                      )
                    )
                  }
                )
              )
            }
          ),
          div(cls := "col-md-4",
            div(cls := "sidebar p-3 bg-light rounded",
              h4("Categories"),
              ul(cls := "list-unstyled",
                articles.flatMap(_.category).distinct.sorted.map(cat =>
                  li(a(href := s"#$cat", cat))
                )
              ),
              hr(),
              h4("About"),
              p("This blog covers topics in Scala, functional programming, and software development best practices."),
              a(href := "/about.html", cls := "btn btn-outline-primary btn-sm", "Learn more")
            )
          )
        )
      ).toString
    )
  }

  def createCustomCSS(): Unit = {
    val cssContent = """
                       |/* General Styling */
                       |body {
                       |  font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif;
                       |  line-height: 1.6;
                       |  color: #333;
                       |}
                       |
                       |a {
                       |  color: #0066cc;
                       |  text-decoration: none;
                       |}
                       |
                       |a:hover {
                       |  text-decoration: underline;
                       |}
                       |
                       |/* Header */
                       |.site-header {
                       |  background-color: #f8f9fa;
                       |  border-bottom: 1px solid #e9ecef;
                       |}
                       |
                       |.site-title {
                       |  font-size: 1.5rem;
                       |  margin: 0;
                       |}
                       |
                       |.site-title a {
                       |  color: #333;
                       |  text-decoration: none;
                       |}
                       |
                       |.site-nav .nav-link {
                       |  color: #555;
                       |  font-weight: 500;
                       |}
                       |
                       |.site-nav .nav-link:hover {
                       |  color: #0066cc;
                       |}
                       |
                       |/* Blog Index */
                       |.year-heading {
                       |  font-size: 1.5rem;
                       |  color: #555;
                       |  border-bottom: 1px solid #eee;
                       |  padding-bottom: 0.5rem;
                       |}
                       |
                       |.article-title {
                       |  font-size: 1.25rem;
                       |  margin-bottom: 0.25rem;
                       |}
                       |
                       |.article-description {
                       |  font-size: 0.9rem;
                       |}
                       |
                       |.article-item {
                       |  padding-bottom: 1rem;
                       |  border-bottom: 1px solid #f0f0f0;
                       |}
                       |
                       |.article-date {
                       |  min-width: 70px;
                       |}
                       |
                       |/* Article Page */
                       |.article-header {
                       |  margin-bottom: 2rem;
                       |}
                       |
                       |.article-meta {
                       |  color: #6c757d;
                       |  font-size: 0.9rem;
                       |}
                       |
                       |.article-content h2 {
                       |  margin-top: 2rem;
                       |  margin-bottom: 1rem;
                       |}
                       |
                       |.article-content h3 {
                       |  margin-top: 1.5rem;
                       |  margin-bottom: 0.75rem;
                       |}
                       |
                       |.article-content pre {
                       |  background-color: #f8f9fa;
                       |  padding: 1rem;
                       |  border-radius: 4px;
                       |  overflow-x: auto;
                       |}
                       |
                       |.article-content img {
                       |  max-width: 100%;
                       |  height: auto;
                       |  display: block;
                       |  margin: 1.5rem auto;
                       |}
                       |
                       |.article-content blockquote {
                       |  border-left: 4px solid #0066cc;
                       |  padding-left: 1rem;
                       |  color: #555;
                       |  font-style: italic;
                       |  margin: 1.5rem 0;
                       |}
                       |
                       |/* Footer */
                       |.site-footer {
                       |  background-color: #f8f9fa;
                       |  border-top: 1px solid #e9ecef;
                       |  color: #6c757d;
                       |}
                       |
                       |.site-footer h5 {
                       |  font-size: 1rem;
                       |  margin-bottom: 1rem;
                       |  color: #495057;
                       |}
                       |
                       |.site-footer a {
                       |  color: #6c757d;
                       |}
                       |
                       |.site-footer a:hover {
                       |  color: #0066cc;
                       |}
                       |
                       |.social-links a {
                       |  font-size: 1.25rem;
                       |  margin-right: 0.5rem;
                       |}
                       |
                       |/* Responsive adjustments */
                       |@media (max-width: 768px) {
                       |  .site-title {
                       |    font-size: 1.25rem;
                       |  }
                       |  
                       |  .article-date {
                       |    min-width: 60px;
                       |  }
                       |  
                       |  .sidebar {
                       |    margin-top: 2rem;
                       |  }
                       |}
    """.stripMargin

    os.makeDir.all(outPath / "css")
    os.write(outPath / "css" / "styles.css", cssContent)
  }

  def renderBlog(): Unit = {
    // Clean up and prepare the output directory
    if (os.exists(outPath)) os.remove.all(outPath)
    os.makeDir.all(outPath / "article")
    os.makeDir.all(outPath / "css")

    // Create custom CSS
    createCustomCSS()

    // Generate article pages
    val articles = os.list(blogRoot)
      .filter(_.ext == "md")
      .map(filePath => renderPost(filePath, outPath))
      .sortBy(_.date)(Ordering[LocalDate].reverse)

    // Generate static pages
    if (os.exists(staticPagesRoot)) {
      os.list(staticPagesRoot)
        .filter(_.ext == "md")
        .foreach(file => renderStaticPage(file, outPath))
    } else {
      // Create default static pages if they don't exist
      createDefaultStaticPages()
    }

    // Generate index page
    renderBlogIndex(articles)
  }

  def createDefaultStaticPages(): Unit = {
    os.makeDir.all(staticPagesRoot)

    // About page
    val aboutContent = """---
                         |title: About
                         |---
                         |
                         |# About Me
                         |
                         |I'm a passionate programmer and educator focused on Scala, functional programming, and building high-performance applications on the JVM.
                         |
                         |## Skills & Expertise
                         |
                         |* Scala and Functional Programming
                         |* Akka and Actor-based systems
                         |* Apache Spark for big data processing
                         |* Cats and Cats Effect for pure functional programming
                         |* ZIO for asynchronous and concurrent applications
                         |
                         |## Background
                         |
                         |I have been working with Scala since 2015 and have helped hundreds of developers master functional programming concepts through my courses and tutorials.
                         |
                         |## Teaching Philosophy
                         |
                         |I believe in learning by doing, focusing on practical examples and real-world applications.
                         |
                         |---
                         |
                         |Want to learn more? Check out my [courses and tutorials](https://rockthejvm.com).
                         |""".stripMargin

    os.write(staticPagesRoot / "about.md", aboutContent)

    // Projects page
    val projectsContent = """---
                            |title: Projects
                            |---
                            |
                            |# Projects
                            |
                            |Here are some of the projects I've been working on:
                            |
                            |## Rock the JVM Courses
                            |
                            |A comprehensive library of courses teaching Scala, Akka, Spark, and functional programming.
                            |
                            |* **Technologies**: Scala, Akka, Cats, ZIO, Spark
                            |* **Link**: [rockthejvm.com](https://rockthejvm.com)
                            |
                            |## Functional Data Structures
                            |
                            |An open-source library implementing various functional data structures in Scala.
                            |
                            |* **Technologies**: Scala, ScalaCheck, Cats
                            |* **GitHub**: [github.com/rockthejvm/functional-data-structures](https://github.com/rockthejvm)
                            |
                            |## Scala Web Framework
                            |
                            |A lightweight web framework built with pure functional programming principles.
                            |
                            |* **Technologies**: Scala, Http4s, Cats Effect, Doobie
                            |* **GitHub**: [github.com/rockthejvm/scala-web-framework](https://github.com/rockthejvm)
                            |""".stripMargin

    os.write(staticPagesRoot / "projects.md", projectsContent)

    // Contact page
    val contactContent = """---
                           |title: Contact
                           |---
                           |
                           |# Get in Touch
                           |
                           |I'd love to hear from you! Whether you have a question about my tutorials, want to discuss a potential collaboration, or just want to say hello, feel free to reach out.
                           |
                           |## Social Media
                           |
                           |* [GitHub](https://github.com/rockthejvm)
                           |* [Twitter](https://twitter.com/rockthejvm)
                           |* [YouTube](https://www.youtube.com/rockthejvm)
                           |* [LinkedIn](https://linkedin.com/in/danielciocirlan)
                           |
                           |## Email
                           |
                           |You can reach me at [daniel@rockthejvm.com](mailto:daniel@rockthejvm.com)
                           |
                           |## Newsletter
                           |
                           |Subscribe to my newsletter to receive updates on new tutorials, courses, and tech insights:
                           |
                           |<form class="mt-3">
                           |  <div class="mb-3">
                           |    <input type="email" class="form-control" placeholder="Your email address">
                           |  </div>
                           |  <button type="submit" class="btn btn-primary">Subscribe</button>
                           |</form>
                           |""".stripMargin

    os.write(staticPagesRoot / "contact.md", contactContent)

    renderStaticPage(staticPagesRoot / "about.md", outPath)
    renderStaticPage(staticPagesRoot / "projects.md", outPath)
    renderStaticPage(staticPagesRoot / "contact.md", outPath)
  }

  def main(args: Array[String]): Unit = {
    renderBlog()
  }
}
