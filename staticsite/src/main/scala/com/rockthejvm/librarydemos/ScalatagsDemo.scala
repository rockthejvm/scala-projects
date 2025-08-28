package com.rockthejvm.librarydemos

import scalatags.Text.all.*

object ScalatagsDemo {
  // composable/reusable components
  def headerComponent(title: String) =
    header(
      cls := "header",
      h1(title),
      div(
        a(href := "#", "Home"),
        a(href := "#", "About"),
      )
    )

  def contentComponent(items: List[String]) =
    div(
      cls := "content",
      h2("Content"),
      ul(
        items.map(item => li(item))
      )
    )

  def footerComponent(copyright: String) =
    div(
      cls := "footer",
      p(copyright)
    )

  def main(args: Array[String]): Unit = {
    val simpleDiv = div("Simple Div content")
    val paragraph = p(
      cls := "my-class", // a Modifier
      id := "first-paragraph", // another Modifier
      "This is a paragraph" // the content = another Modifier
    )

    println(simpleDiv.render) // <div>Simple Div content</div>
    println(paragraph.render) // <p class="my-class" id="first-paragraph">This is a paragraph.</p>

    // nested elements
    val nested =
      div(
        cls := "container",
        title := "Title of this div",
        h1("Scalatags Demo"),
        ul(
          li("HTML rendering"),
          li("nested")
        )
      )

    println(nested.render)

    // entire HTML pages
    val page = html(
      head(
        tag("title")("Demo page"),
        tag("meta")(charset := "UTF-8")
      ),
      body(
        headerComponent("Rock the JVM"),
        contentComponent(List("Courses", "Articles", "Videos")),
        footerComponent("© 2025 Rock the JVM")
      )
    )

    println(page.render)
  }
}
