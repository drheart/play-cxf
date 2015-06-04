import play.Play.autoImport._
import PlayKeys._

name := "play-cxf"

organization := "eu.imind"

version := "1.1"

scalaVersion := "2.11.1"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

libraryDependencies += "org.springframework" % "spring-context" % "[3.2.0.RELEASE,)"

libraryDependencies += "org.apache.cxf" % "cxf-api" % "2.7.7"

libraryDependencies += "org.apache.cxf" % "cxf-rt-core" % "2.7.7"

libraryDependencies += "javax.inject" % "javax.inject" % "1"

libraryDependencies += "org.apache.cxf" % "cxf-rt-frontend-jaxws" % "2.7.7"

libraryDependencies += "org.apache.cxf" % "cxf-rt-ws-security" % "2.7.7"

libraryDependencies += "org.apache.ws.security" % "wss4j" % "1.6.12"

libraryDependencies += "xml-apis" % "xml-apis" % "1.4.01"
