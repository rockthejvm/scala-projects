package com.rockthejvm.filesync

import castor.*
import java.io.DataOutputStream

sealed trait Msg
case class FileChangeEvent(subject: os.SubPath, deleted: Boolean) extends Msg
case class AgentReply(value: AgentReport) extends Msg

class SyncActor(src: os.Path, out: DataOutputStream)(using Context) extends SimpleActor[Msg] {
  override def run(msg: Msg): Unit =
    msg match {
      case FileChangeEvent(subpath, false) =>
        Protocol.send(out, RequestReport(subpath)) // ask the agent for a hash of its dest subpath
      case FileChangeEvent(subpath, true) =>
        Protocol.send(out, Delete(subpath)) // tell the agent to delete this path
      case AgentReply(AgentReport(subpath, hash)) =>
        // compare the hash from the agent with my own
        Protocol.hashPath(src / subpath) match {
          case None => // it's a folder => tell the agent to make that folder
            Protocol.send(out, CreateDirectory(subpath))
          case `hash` => // noop
          case Some(_) => // it is a file and it is DIFFERENT => tell the agent to OVERWRITE the file
            Protocol.send(out, Overwrite(subpath, os.read.bytes(src / subpath)))
        }
    }
}