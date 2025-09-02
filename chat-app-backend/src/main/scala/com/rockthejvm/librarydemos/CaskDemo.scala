package com.rockthejvm.librarydemos

import cask.*
import cask.router.{EndpointMetadata, Result}
import upickle.default.*

import scala.util.Random

object CaskDemo extends MainRoutes {
  // override val port = 1234 // by default runs on 8080

  @cask.get("/health")
  def healthRoute() =
    "All good!" // payload of the HTTP response

  // path segments
  @cask.get("/find/:name/:id") // "name" must match the "name" argument in the function
  def parsePathSegment(id: Int, name: String) =
    s"Parsing $name successful with id $id"

  // query parameters
  @get("/search") // localhost:8080/search?query=functionalprogramming&limit=25
  def search(query: String, limit: Int = 10) =
    s"Searching for '$query' with limit $limit"
  // query params must have either a default value OR must be supplied when called

  // post/get with JSON
  case class Person(name: String, age: Int) derives ReadWriter

  @get("/person/:name")
  def findUser(name: String) =
    write(Person(name, Random.nextInt(120)))

  @getJson("/person_v2/:name")
  def findUser_v2(name: String) =
    Person(name, Random.nextInt(120))

  @post("/person")
  def addPerson(req: Request) =
    s"Trying to add the person ${read[Person](req.text())}"

  // post localhost:8080/person_v2 --data {"name":"daniel", "age": 87}
  @postJson("/person_v2")
  def addPerson_v2(name: String, age: Int) =
    s"Trying to add the person ${Person(name, age)}"

  // post localhost:8080/person_v3 --data '{"person": {"name":"daniel", "age": 87}}
  @postJson("/person_v3")
  def addPerson_v3(person: Person) =
    s"Trying to add the person $person"

  // static files
  // get localhost:8080/static/CaskDemo.scala
  @staticFiles("/static")
  def getFiles() =
    (os.pwd / "chat-app-backend/src/main/scala/com/rockthejvm/librarydemos").toString

  // websockets
  @websocket("/ws")
  def ws() = cask.WsHandler { channel =>
    cask.WsActor {
      case cask.Ws.Text(msg) =>
        channel.send(cask.Ws.Text(s"You said: $msg"))
    }
  }

  // error handling
  @get("/fail")
  def fail() =
    throw new RuntimeException("boom!")

  override def handleEndpointError(routes: Routes, metadata: EndpointMetadata[_], e: Result.Error, req: Request) = 
    s"Something broke: $e"

  println(s"Rock the JVM! Server ready.")
  initialize()
}
