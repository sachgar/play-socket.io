import Dependencies.AkkaVersion
import Dependencies.Scala212
import Dependencies.Scala213
import Dependencies.Scala3
import Dependencies.PlayVersion

lazy val runChromeWebDriver = taskKey[Unit]("Run the chromewebdriver tests")

val macwire = "com.softwaremill.macwire" %% "macros" % "2.5.7"
val lombok  = "org.projectlombok"         % "lombok" % "1.18.8" % Provided
val akkaCluster = Seq(
  "com.typesafe.akka" %% "akka-cluster"       % AkkaVersion,
  "com.typesafe.akka" %% "akka-cluster-tools" % AkkaVersion
)

// Customise sbt-dynver's behaviour to make it work with tags which aren't v-prefixed
(ThisBuild / dynverVTagPrefix) := false

lazy val root = (project in file("."))
  .settings(
    organization := "com.typesafe.play",
    name := "play-socket-io",
    mimaPreviousArtifacts := Set.empty, // TODO: enable after first release
    scalaVersion := Scala213,
    crossScalaVersions := Seq(Scala213, Scala212, Scala3),
    scalacOptions ++= {
      CrossVersion.partialVersion(scalaVersion.value) match {
        case Some((3, n))   =>  Seq("-feature", "-release", "11", "-rewrite", "-source:3.3-migration", "-explain")
        case _              =>  Seq("-feature", "-release", "11", "-Xsource:3")
      }
    },
    (Compile / doc / scalacOptions) := Nil,
    javacOptions ++= Seq("-Xlint"),
    libraryDependencies ++= Seq(
      // Production dependencies
      "com.typesafe.play" %% "play"        % PlayVersion,
      "com.typesafe.akka" %% "akka-remote" % AkkaVersion,
      // Test dependencies for running a Play server
      "com.typesafe.play" %% "play-akka-http-server" % PlayVersion % Test,
      "com.typesafe.play" %% "play-logback"          % PlayVersion % Test,
      // Test dependencies for Scala/Java dependency injection
      "com.typesafe.play" %% "play-guice" % PlayVersion % Test,
      macwire              % Test,
      // Test dependencies for running chrome driver
      "io.github.bonigarcia"    % "webdrivermanager"       % "5.3.2" % Test,
      "org.seleniumhq.selenium" % "selenium-chrome-driver" % "4.5.3" % Test,
      // Test framework dependencies
      "org.scalatest" %% "scalatest"       % "3.2.16" % Test,
      "com.novocode"   % "junit-interface" % "0.11"  % Test
    ),
    (Compile / PB.targets) := Seq(
      scalapb.gen() -> (Compile / sourceManaged).value
    ),
    (Test / fork) := true,
    (Test / run / connectInput) := true,
    runChromeWebDriver := {
      (Test / runMain).toTask(" play.socketio.RunSocketIOTests").value
    },
    TaskKey[Unit]("runJavaServer") :=
      (Test / runMain).toTask(" play.socketio.javadsl.TestSocketIOJavaApplication").value,
    TaskKey[Unit]("runScalaServer") :=
      (Test / runMain).toTask(" play.socketio.scaladsl.TestSocketIOScalaApplication").value,
    TaskKey[Unit]("runMultiNodeServer") :=
      (Test / runMain).toTask(" play.socketio.scaladsl.TestMultiNodeSocketIOApplication").value,
    (Test / test) := {
      (Test / test).value
      runChromeWebDriver.value
    },
    resolvers += "jitpack".at("https://jitpack.io"),
    headerLicense := Some(
      HeaderLicense.Custom(
        "Copyright (C) from 2022 The Play Framework Contributors <https://github.com/playframework>, 2011-2021 Lightbend Inc. <https://www.lightbend.com>"
      )
    ),
    headerEmptyLine := false
  )

lazy val scalaChat = (project in file("samples/scala/chat"))
  .enablePlugins(PlayScala)
  .dependsOn(root)
  .settings(
    name := "play-socket.io-scala-chat-example",
    organization := "com.typesafe.play",
    scalaVersion := Scala213,
    crossScalaVersions := Seq(Scala213, Scala212, Scala3),
    libraryDependencies += macwire % Provided
  )

lazy val scalaMultiRoomChat = (project in file("samples/scala/multi-room-chat"))
  .enablePlugins(PlayScala)
  .dependsOn(root)
  .settings(
    name := "play-socket.io-scala-multi-room-chat-example",
    organization := "com.typesafe.play",
    scalaVersion := Scala213,
    crossScalaVersions := Seq(Scala213, Scala212, Scala3),
    libraryDependencies += macwire % Provided
  )

lazy val scalaClusteredChat = (project in file("samples/scala/clustered-chat"))
  .enablePlugins(PlayScala)
  .dependsOn(root)
  .settings(
    name := "play-socket.io-scala-clustered-chat-example",
    organization := "com.typesafe.play",
    scalaVersion := Scala213,
    crossScalaVersions := Seq(Scala213, Scala212, Scala3),
    libraryDependencies ++= Seq(macwire % Provided) ++ akkaCluster
  )

lazy val javaChat = (project in file("samples/java/chat"))
  .enablePlugins(PlayJava)
  .dependsOn(root)
  .settings(
    name := "play-socket.io-java-chat-example",
    organization := "com.typesafe.play",
    scalaVersion := Scala213,
    crossScalaVersions := Seq(Scala213, Scala212, Scala3),
    libraryDependencies += guice
  )

lazy val javaMultiRoomChat = (project in file("samples/java/multi-room-chat"))
  .enablePlugins(PlayJava)
  .dependsOn(root)
  .settings(
    name := "play-socket.io-java-multi-room-chat-example",
    organization := "com.typesafe.play",
    scalaVersion := Scala213,
    crossScalaVersions := Seq(Scala213, Scala212, Scala3),
    libraryDependencies ++= Seq(guice, lombok)
  )

lazy val javaClusteredChat = (project in file("samples/java/clustered-chat"))
  .enablePlugins(PlayJava)
  .dependsOn(root)
  .settings(
    name := "play-socket.io-java-clustered-chat-example",
    organization := "com.typesafe.play",
    scalaVersion := Scala213,
    crossScalaVersions := Seq(Scala213, Scala212, Scala3),
    libraryDependencies ++= Seq(guice, lombok) ++ akkaCluster
  )

addCommandAlias(
  "validateCode",
  (
    List(
      "headerCheckAll",
      "scalafmtSbtCheck",
      "scalafmtCheckAll",
    ) ++
      List(scalaChat, scalaMultiRoomChat, scalaClusteredChat, javaChat, javaMultiRoomChat, javaClusteredChat)
        .flatMap(p => List("scalafmtCheckAll").map(cmd => s"${p.id}/$cmd"))
  ).mkString(";")
)
