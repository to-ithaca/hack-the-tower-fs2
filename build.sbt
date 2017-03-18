name := "authentication-service"

scalaVersion := "2.12.1"

organization := "org.typelevel"

scalacOptions ++= Seq(
  "-Ypartial-unification"
)

lazy val fs2Version = "0.9.4"

resolvers ++= Seq(
  Resolver.sonatypeRepo("snapshots")
)

libraryDependencies ++= Seq(
  "co.fs2" %% "fs2-core" % fs2Version,
  "co.fs2" %% "fs2-io" % fs2Version,
  "org.scalatest" %% "scalatest" % "3.0.1"
)
