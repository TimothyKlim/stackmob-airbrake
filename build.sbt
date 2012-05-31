name := "stackmob-airbrake"

organization := "com.stackmob"

version := "0.1.0-SNAPSHOT"

scalaVersion := "2.9.1"

crossScalaVersions := Seq("2.9.1")

scalacOptions ++= Seq("-unchecked", "-deprecation")

libraryDependencies ++= {
  Seq(
    "org.scalaz" %% "scalaz-core" % "6.0.3" withSources(),
    "org.specs2" %% "specs2" % "1.9" % "test" withSources()
  )
}

logBuffered := false
