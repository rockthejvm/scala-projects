package com.rockthejvm.filesync

import java.io.{DataInputStream, DataOutputStream}

object Agent {
  // performs actions on the dest dir
  // cwd = dest dir

  // read messages in binary from stdin
  // write messages in binary to stdout

  def main(args: Array[String]): Unit = {
    val input = new DataInputStream(System.in)
    val output = new DataOutputStream(System.out)

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
  }
}
