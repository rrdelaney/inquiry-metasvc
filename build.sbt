name := """metasvc"""
version := "1.0-SNAPSHOT"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.7"

libraryDependencies ++= Seq(
  cache,
  ws,
  specs2 % Test,
  filters,
  "org.postgresql" % "postgresql" % "9.4-1204-jdbc4",
  "com.typesafe.play" %% "play-slick" % "1.1.0"
)

seq(flywaySettings: _*)
flywayUrl := "jdbc:postgresql://localhost:5432/inquiry_db"
flywayUser := "inquiry"
flywayPassword := "inquiry"
flywayLocations := Seq("filesystem:./migrations")

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"
resolvers += Resolver.url("Typesafe Ivy releases", url("https://repo.typesafe.com/typesafe/ivy-releases"))(Resolver.ivyStylePatterns)

routesGenerator := InjectedRoutesGenerator


fork in run := true
