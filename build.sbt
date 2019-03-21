import CompilerFlags._

name := "akka-micro-blog"
organization := "me.rotemfo"
version := "1.0"

mainClass := Some("me.rotemfo.Main")

scalaVersion := "2.12.8"

scalacOptions ++= compilerFlags
scalacOptions in(Compile, console) ~= filterExcludedReplOptions

lazy val akkaVersion = "2.5.21"
lazy val akkaHttpVersion = "10.1.7"
lazy val circeVersion = "0.11.1"
lazy val akkaJsonVersion = "1.25.2"

libraryDependencies ++= Seq(
  "com.typesafe.akka"        %% "akka-actor"       % akkaVersion,
  "com.typesafe.akka"        %% "akka-persistence" % akkaVersion,
  "com.typesafe.akka"        %% "akka-stream"      % akkaVersion,
  "com.typesafe.akka"        %% "akka-http"        % akkaHttpVersion,
  "io.circe"                 %% "circe-generic"    % circeVersion,
  "de.heikoseeberger"        %% "akka-http-circe"  % akkaJsonVersion,
  "org.iq80.leveldb"          % "leveldb"          % "0.11",
  "org.fusesource.leveldbjni" % "leveldbjni-all"   % "1.8",
  "com.typesafe.akka"        %% "akka-testkit"     % akkaVersion      % Test,
  "org.scalatest"            %% "scalatest"        % "3.0.7"          % Test
)
