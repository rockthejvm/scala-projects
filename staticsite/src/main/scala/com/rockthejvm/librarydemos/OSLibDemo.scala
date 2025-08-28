package com.rockthejvm.librarydemos

import os.*

object OSLibDemo {
  def main(args: Array[String]): Unit = {
    // os.__ => handy paths
    println("current directory: " + os.pwd)
    println("home directory: " + os.home)

    // make directory in the tmp
    val tempDir = os.temp.dir(prefix = "os-lib-demo-")
    println("created temp directory: " + tempDir)

    // writing a file
    val testFile = tempDir / "test.txt"
    os.write(testFile, "Hello from OS-lib, I'm writing to a file")
    println("written to file: " + testFile)

    // read file content
    val content: String = os.read(testFile)
    println("file content: " + content)

    // list files in directory
    os.write(tempDir / "anotherFile.txt", "Some more content")
    println("Files in the temp directory:")
    os.list(tempDir).foreach(path => println(s"- $path"))

    // copy files
    val copyDest = tempDir / "test-copy.txt"
    os.copy(testFile, copyDest)

    // check if the file exists
    println("Does the copy file exist? " + os.exists(copyDest))

    // check file properties
    println("Copy file size: " + os.size(copyDest)) // returns size in bytes

    /*
      Processes
     */
    println("---- Running processes ----")

    // invoke a process
    val result = os.proc("echo", "Hello from secondary process").call()
    println("process exit code: " + result.exitCode)
    println("process output: " + result.out.text())

    // run command within a particular directory
    val lsResult = os.proc("ls", "-la").call(cwd = tempDir)
    println("Dir contents: " + lsResult.out.text())

    // clean up everything
    os.remove.all(tempDir)
    println("Cleaned everything.")
  }
}
