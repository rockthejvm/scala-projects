package com.rockthejvm.staticsite

object BasicServer extends cask.MainRoutes {
  override val port = 4321

  // build the blog
  ExpandedWebsite.renderBlog()

  // specify the static file root
  val resourcePath = os.Path("/Users/daniel/dev/rockthejvm/courses/scala-projects/staticsite/src/main/resources")
  val blogDir = resourcePath / "blog_expanded_out"

  @cask.staticFiles("/")
  def staticFileRoutes() = blogDir.toString

  // start the server
  initialize()
}
