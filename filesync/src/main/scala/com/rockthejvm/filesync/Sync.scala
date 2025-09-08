package com.rockthejvm.filesync

import castor.*

import java.io.DataOutputStream
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

object Sync {
  given Context = new Context.Simple(
    ExecutionContext.fromExecutorService(
      Executors.newFixedThreadPool(4)
    ),
    _.printStackTrace()
  )

  // watch over the source dir
  def main(args: Array[String]): Unit = {
    // Sync sourceDir destDir
    if (args.length != 2) {
      println("usage: sync <srcDir> <destDir>")
      System.exit(1)
    }

    val src = os.Path(args(0))
    val dest = os.Path(args(1))

    if (!os.exists(src)) {
      println(s"source dir $src does not exist")
      System.exit(2)
    }

    println(s"[sync] syncing $src with $dest...")

    // child Agent process which performs the actions on the dest dir
    val agentExecutable = os.temp(os.read.bytes(os.resource / "agent/agent.jar"))
    os.perms.set(agentExecutable, "rwxr-xr-x")
    val agent = os.proc("java", "-jar", agentExecutable).spawn(cwd = dest)

    /*
      - actor
        - receives change events to the src dir
        - with every new change => send an Rpc to the agent
     */
    val syncActor = new SyncActor(src, agent.stdin.data)

    new Thread(() => {
      while(agent.isAlive()) {
        // wait for reports from the agent
        val report = Protocol.receive[AgentReport](agent.stdout.data)
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
