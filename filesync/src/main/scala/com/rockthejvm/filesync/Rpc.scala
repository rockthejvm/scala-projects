package com.rockthejvm.filesync

import upickle.default.*
import java.io.{DataInputStream, DataOutputStream}
import Protocol.given 

sealed trait Rpc derives ReadWriter
case class CreateDirectory(path: os.SubPath) extends Rpc derives ReadWriter
case class Overwrite(path: os.SubPath, bytes: Array[Byte]) extends Rpc derives ReadWriter // also can create new gile
case class Delete(path: os.SubPath) extends Rpc derives ReadWriter

case class AgentReport(path: os.SubPath, fileHash: Option[Int]) extends Rpc derives ReadWriter
case class RequestReport(path: os.SubPath) extends Rpc derives ReadWriter

// TODO - add all commands to send to the agent: create folder, overwrite, ...

object Protocol {
  given ReadWriter[os.SubPath] =
    readwriter[String].bimap[os.SubPath](
      _.toString, // subpath to string
      os.SubPath(_) // string to subpath
    )
  
  def send[T: Writer](out: DataOutputStream, msg: T): Unit = {
    val bytes: Array[Byte] = writeBinary(msg)
    out.writeInt(bytes.length)
    out.write(bytes)
    out.flush()
  }

  def receive[T: Reader](in: DataInputStream): T = {
    val nBytes = in.readInt()
    val buf = new Array[Byte](nBytes)
    in.readFully(buf)
    readBinary[T](buf)
  }

  def hashPath(p: os.Path): Option[Int] =
    if (!os.isFile(p)) None
    else {
      val bytes = os.read.bytes(p)
      Some(java.util.Arrays.hashCode(bytes))
    }
}
