package com.rockthejvm.filesync

import castor.*

import java.io.{DataInputStream, DataOutputStream}
import java.net.ServerSocket
import java.util.concurrent.Executors
import scala.concurrent.ExecutionContext

/**
 * - build the application with Agent as main
 * - run Sync
 *
 * - spawn child actor process with agent.jar as the executable
 * - with every file change
 *    - send a file change event to the sync actor
 *      - sync actor will send a request for a hash change of the contents subject to the event
 *          - agent sends back a report (to the parent process) => Sync sends AgentReply to the actor
     *        - if we get the same hash => noop
     *        - else - tell the actor to mirror the file
 */

object RemoteDrive {
  given Context = new Context.Simple(
    ExecutionContext.fromExecutorService(
      Executors.newFixedThreadPool(4)
    ),
    _.printStackTrace()
  )

  // watch over the source dir
  def main(args: Array[String]): Unit = {
    // Sync sourceDir port
    if (args.length != 2) {
      println("usage: remoteDrive <srcDir> <port>")
      System.exit(1)
    }

    val src = os.Path(args(0))
    val port = args(1).toInt

    if (!os.exists(src)) {
      println(s"source dir $src does not exist")
      System.exit(2)
    }

    println(s"[sync] waiting for agent to replicate $src...")

    /*
      - open a port and listen to it
      - another process (RemoteAgent) will start and connect to this port (replica)
     */
    val serverSocket = new ServerSocket(port)
    println(s"[sync] server started, waiting for connection on port $port")
    
    val socket = serverSocket.accept() // blocking
    println("[sync] client connected")
    
    val dataOut = new DataOutputStream(socket.getOutputStream)
    val dataIn = new DataInputStream(socket.getInputStream)

    /*
      - actor
        - receives change events to the src dir
        - with every new change => send an Rpc to the agent
     */
    val syncActor = new SyncActor(src, dataOut)

    new Thread(() => {
      while(!socket.isClosed) {
        // wait for reports from the agent
        val report = Protocol.receive[AgentReport](dataIn)
        // forward them to the actor
        syncActor.send(AgentReply(report))
      }
    }).start()

    os.watch.watch(
      List(src),
      onEvent = _.foreach { affectedPath =>
        val subPath = affectedPath.subRelativeTo(src)
        println(s"[sync] path affected: $subPath")

        syncActor.send(FileChangeEvent(subPath, !os.exists(src / subPath)))
      }
    )

    Thread.sleep(Long.MaxValue)
  }
}
