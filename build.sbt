ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "3.7.1"

lazy val scrapeyard = (project in file("scrapeyard"))
  .settings(
    name := "scrapeyard",
    libraryDependencies ++= Seq(
      "org.jsoup" % "jsoup" % "1.21.1",
      "com.lihaoyi" %% "requests"  % "0.9.0",
      "com.lihaoyi" %% "upickle"  % "4.2.1",
      "org.scala-lang.modules" %% "scala-parallel-collections" % "1.2.0",
      "com.github.pureconfig" %% "pureconfig-core" % "0.17.9",
      // java libraries
      "org.quartz-scheduler"    % "quartz"                     % "2.5.0",
      "org.quartz-scheduler"    % "quartz-jobs"                % "2.5.0",
      "com.sun.mail"            % "javax.mail"                 % "1.6.2",
      "ch.qos.logback" % "logback-classic" % "1.5.18"
    )
  )

lazy val photoscala = (project in file("photoscala"))
  .settings(
    name := "photoscala"
  )

lazy val staticsite = (project in file("staticsite"))
  .settings(
    name := "staticsite",
    libraryDependencies ++= Seq(
      "com.lihaoyi" %% "os-lib"    % "0.11.4",
      "com.lihaoyi" %% "scalatags" % "0.13.1",
      "com.lihaoyi" %% "cask"      % "0.10.2",
      // markdown
      "org.commonmark" % "commonmark" % "0.24.0",
    )
  )

lazy val chatApp =
  crossProject(JSPlatform, JVMPlatform)
    .in(file("chat-app"))
    .settings(
      name := "chat-app",
    )
    .jsSettings(
      name := "frontend",
      Compile / fastOptJS / artifactPath := baseDirectory.value / "static/main.js",
      scalaJSUseMainModuleInitializer    := true,
      libraryDependencies ++= Seq(
        "org.scala-js"  %%% "scalajs-dom" % "2.8.0",
        "com.lihaoyi" %%% "scalatags"   % "0.13.1",
        "com.lihaoyi" %%% "upickle"   % "4.3.0",
      )
    )
    .jvmSettings(
      name := "backend",
      fork := true,
      libraryDependencies ++= Seq(
        "com.lihaoyi" %% "os-lib"    % "0.11.5",
        "com.lihaoyi" %% "scalatags" % "0.13.1",
        "com.lihaoyi" %% "cask"      % "0.10.2",
        "com.lihaoyi" %% "upickle"   % "4.3.0",
        "com.lihaoyi" %% "scalasql" % "0.1.20",
        "com.lihaoyi" %% "scalasql-namedtuples" % "0.1.20",
        "org.postgresql" % "postgresql" % "42.7.7",
      )
    )


lazy val root = (project in file("."))
  .settings(
    name := "scala-projects"
  )
