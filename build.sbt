// Copyright (c) Microsoft. All rights reserved.

name := "azure-eventhubs-reactive"
organization := "com.microsoft.azure"

version := "0.5.0"

scalaVersion := "2.12.10"
crossScalaVersions := Seq("2.11.8", "2.12.10")

libraryDependencies ++= {
  Seq(
    // https://github.com/Azure/azure-event-hubs-java/releases
    "com.microsoft.azure" % "azure-eventhubs" % "0.15.0",

    // https://github.com/Azure/azure-storage-java/releases
    "com.microsoft.azure" % "azure-storage" % "5.0.0",

    // https://github.com/datastax/java-driver/releases
    "com.datastax.cassandra" % "cassandra-driver-core" % "3.1.4",

    // https://github.com/akka/akka/releases
    "com.typesafe.akka" %% "akka-stream" % "2.5.26",

    // https://github.com/json4s/json4s/releases
    "org.json4s" %% "json4s-native" % "3.5.1",
    "org.json4s" %% "json4s-jackson" % "3.5.1"
  )
}

// Test dependencies
libraryDependencies ++= Seq(
  // https://github.com/scalatest/scalatest/releases
  "org.scalatest" %% "scalatest" % "3.2.0-SNAP4" % "test",

  // http://search.maven.org/#search%7Cgav%7C1%7Cg%3A%22org.mockito%22%20AND%20a%3A%22mockito-all%22
  "org.mockito" % "mockito-all" % "1.10.19" % "test"
)

lazy val reactiveEventHubs = project.in(file(".")).configs(IntegrationTest)
lazy val samplesScala = project.in(file("samples-scala")).dependsOn(reactiveEventHubs)
lazy val samplesJava = project.in(file("samples-java")).dependsOn(reactiveEventHubs)

/* Publishing options
 * see http://www.scala-sbt.org/0.13/docs/Artifacts.html
 */
publishArtifact in Test := true
publishArtifact in(Compile, packageDoc) := true
publishArtifact in(Compile, packageSrc) := true
publishArtifact in(Compile, packageBin) := true

// Note: for Bintray, unpublish using SBT
licenses += ("MIT", url("https://github.com/Azure/reactive-event-hubs-java/blob/master/LICENSE"))
publishMavenStyle := true

// Required in Sonatype
pomExtra :=
  <url>https://github.com/Azure/reactive-event-hubs-java</url>
    <scm>
      <url>https://github.com/Azure/reactive-event-hubs-java</url>
    </scm>
    <developers>
      <developer>
        <id>microsoft</id>
        <name>Microsoft</name>
        <organization>Microsoft</organization>
        <organizationUrl>http://www.microsoft.com</organizationUrl>
      </developer>
    </developers>

/** Miscs
  */
logLevel := Level.Info // Debug|Info|Warn|Error - `Info` provides a better output with `sbt test`.
scalacOptions ++= Seq("-deprecation", "-explaintypes", "-unchecked", "-feature")
showTiming := true
fork := true
parallelExecution := true
