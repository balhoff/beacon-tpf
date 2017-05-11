enablePlugins(JavaServerAppPackaging)

organization  := "io.ncats.translator"

name          := "beacon-tpf"

version       := "0.0.1"

publishArtifact in Test := false

licenses := Seq("BSD-3-Clause" -> url("https://opensource.org/licenses/BSD-3-Clause"))

homepage := Some(url("https://github.com/balhoff/beacon-tpf"))

scalaVersion  := "2.11.8"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

scalacOptions in Test ++= Seq("-Yrangepos")

mainClass in Compile := Some("io.ncats.translator.beacon.Main")

javaOptions += "-Xmx10G"

fork in Test := true

libraryDependencies ++= {
  Seq(
    "com.typesafe.akka"           %% "akka-http"              % "10.0.6",
    "com.typesafe.akka"           %% "akka-http-spray-json"   % "10.0.4",
    "ch.megard"                   %% "akka-http-cors"         % "0.2.1",
    "io.spray"                    %%  "spray-json"            % "1.3.3",
    "org.apache.jena"             %  "apache-jena-libs"       % "3.2.0" pomOnly(),
    "com.typesafe.scala-logging"  %% "scala-logging"          % "3.4.0",
    "ch.qos.logback"              %  "logback-classic"        % "1.1.7",
    "org.codehaus.groovy"         %  "groovy-all"             % "2.4.6"
  )
}

