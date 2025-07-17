ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.7.1"

lazy val scrapeyard = (project in file("scrapeyard"))
  .settings(
    name := "scrapeyard",
    libraryDependencies ++= Seq(
      "org.jsoup" % "jsoup" % "1.21.1",
      "com.lihaoyi" %% "requests"  % "0.9.0",
      "org.scala-lang.modules" %% "scala-parallel-collections" % "1.2.0",
      "org.quartz-scheduler"    % "quartz"                     % "2.5.0",
      "org.quartz-scheduler"    % "quartz-jobs"                % "2.5.0",
      "com.sun.mail"            % "javax.mail"                 % "1.6.2"
    )
  )

lazy val photoscala = (project in file("photoscala"))
  .settings(
    name := "photoscala"
  )

lazy val root = (project in file("."))
  .settings(
    name := "scala-projects"
  )
