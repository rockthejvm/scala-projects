package com.rockthejvm.scrapeyard

object HttpApis {
  def main(args: Array[String]): Unit = {
    val firstResponse = requests.get("https://api.github.com/users/lihaoyi")
    println(firstResponse.statusCode)
    println(firstResponse.headers("content-type"))
    println(firstResponse.text())

    // all http methods
    // httpbin.org/post?user=daniel&password=rockthejvm
    val r1 = requests.post("http://httpbin.org/post", params = Map("user" -> "daniel", "password" -> "rockthejvm"))
    // httpbin.org/put { "data":"daniel", "password":"rockthejvm" }
    val r2 = requests.put("http://httpbin.org/put", data = Map("user" -> "daniel", "password" -> "rockthejvm"))
    // delete, options

    val json = ujson.read(firstResponse.text())
    println(json.obj.keySet)
    println(json.obj.get("email").filter(!_.isNull)) // careful with nulls
  }
}
