package com.rockthejvm.librarydemos

import castor.*
import sourcecode.{FileName, Line}

import java.time.Duration
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

object CastorDemo {
  // actors are SCHEDULED on a thread pool
  given Context = new Context.Simple(
    ExecutionContext.fromExecutorService( // thread pool
      Executors.newFixedThreadPool(8)
    ),
    _.printStackTrace() // error logger
  )

  def demoPrinter(): Unit = {
    /*
      Actor = object which you can send messages to (async)
     */
    class Printer(using Context) extends SimpleActor[String] { // Context <: ExecutionContext
      // "onMessage"
      override def run(msg: String): Unit =
        println(s"[printer] $msg")
    }
    val printer = new Printer

    // send messages to this actor
    printer.send("Rock the JVM!") // will be added to this actor's "mailbox"
  }

  def demoUploadActor(): Unit = {
    class SimpleUploadActor(using Context) extends SimpleActor[String] {
      private var count = 0

      override def run(msg: String): Unit = {
        println(s"uploading $msg")
        val res = requests.post("http://httpbin.org/post", data = msg)
        count += 1
        println(s"response $count ${res.statusCode} - ${ujson.read(res)("data")}" )
      }
    }

    val uploadActor = new SimpleUploadActor
    (1 to 10).foreach(i => uploadActor.send(s"message $i"))
  }

  // batch
  def demoBatchActor(): Unit = {
    class BatchUploadActor(using Context) extends BatchActor[String] {
      private var totalCount = 0
      private var batchCount = 0
      // make sure that in your logic
      // runBatch(seq) == seq.foreach(run)
      override def runBatch(msgs: Seq[String]): Unit = {
        msgs.foreach { msg =>
          val res = requests.post("http://httpbin.org/post", data = msg)
          println(s"response $totalCount ${res.statusCode} - ${ujson.read(res)("data")}")
        }

        totalCount += msgs.size
        batchCount += 1
      }
    }
  }

  // state machine
  def demoStateMachineActor(): Unit = {
    sealed trait Msg
    case class Text(s: String) extends Msg
    case object Reset extends Msg

    // "scala is awesome" => 3
    // "rock the jvm" => 6
    // "this is just amazing" => 10
    // Reset => 0
    class WordCounter(using Context) extends SimpleActor[Msg] {
      private var totalWordCount = 0

      override def run(msg: Msg): Unit = msg match {
        case Text(s) => totalWordCount += s.split(" ").count(_.nonEmpty)
        case Reset => totalWordCount = 0
      }
    }

    /*
      MyState(0)

      "scala is awesome" => MyState(3)
      "rock the jvm" => MyState(6)
      "this is just amazing" => MyState(10)
      Reset => MyState(0)

     */
    class WordCounterV2(using Context) extends StateMachineActor[Msg] {
      // (state + value) => new state
      // state.change(value) => new state
      case class MyState(totalWordCount: Int) extends State({
        case Text(s) => MyState(totalWordCount + s.split(" ").count(_.nonEmpty))
        case Reset => MyState(0)
      })

      override protected def initialState = MyState(0)
    }
  }

  // actor pipelines
  def demoActorPipeline(): Unit = {
    class Person(val name: String, val friends: List[Person])(using Context) extends SimpleActor[String] {
      override def run(msg: String): Unit = {
        println(s"[$name] I've received $msg")
        if (friends.nonEmpty) println(s"[$name] sending this to my friends too: ${friends.map(_.name).mkString(",")}!")

        friends.foreach(_.send(msg))
      }
    }

    val alice = new Person("Alice", List())
    val bob = new Person("Bob", List(alice))
    val charlie = new Person("Charlie", List())
    val mary = new Person("Mary", List(alice, bob, charlie))

    mary.send("I've just heard about the new Scala Projects course!")
  }

  /*
    Actors are best for 
      - unidirectional data flow
      - preserving order
      - maintaining state
    vs 
    Futures where you expect a result
   */

  def main(args: Array[String]): Unit = {
    demoActorPipeline()
  }
}
