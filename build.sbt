scalaVersion := "2.11.7"
version := "0.20160123"
enablePlugins(JettyPlugin)

libraryDependencies += "javax.servlet" % "javax.servlet-api" % "3.0.1" % "provided"
libraryDependencies += "org.scalatra" % "scalatra_2.11" % "2.4.0"
libraryDependencies += "org.mnode.ical4j" % "ical4j" % "1.0.7"
