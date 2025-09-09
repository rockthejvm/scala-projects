package com.rockthejvm.filesync

import java.io.{DataInputStream, DataOutputStream}
import java.net.Socket

object RemoteAgent {
  // performs actions on the dest dir
  // cwd = dest dir

  // read messages in binary from stdin
  // write messages in binary to stdout

  def main(args: Array[String]): Unit = {
    if (args.length != 2) { // agentCmd host port
      println("[agent] usage: agentCmd <host> <port>")
      System.exit(1)
    }

    val host = args(0)
    val port = args(1).toInt

    try {
      val socket = new Socket(host, port) // block until the conn is established
      // at this point we have a connection
      val input = new DataInputStream(socket.getInputStream)
      val output = new DataOutputStream(socket.getOutputStream)

      while(true) try {
        val message = Protocol.receive[Rpc](input)
        message match {
          case RequestReport(subpath) =>
            Protocol.send(output, AgentReport(subpath, Protocol.hashPath(os.pwd / subpath)))
          case CreateDirectory(subpath) =>
            os.makeDir.all(os.pwd / subpath)
          case Overwrite(subpath, bytes) =>
            os.remove.all(os.pwd / subpath)
            os.write.over(os.pwd / subpath, bytes, createFolders = true)
          case Delete(subPath) =>
            os.remove.all(os.pwd / subPath)
        }
      } catch {
        case e: Exception =>
          e.printStackTrace()
          System.exit(1)
      }
    } catch {
      case e: Exception =>
        println("[agent] failed to connect to remote drive")
        e.printStackTrace()
        System.exit(1)
    }
  }
}
