// The Play plugin
addSbtPlugin("com.typesafe.play" % "sbt-plugin" % "2.4.3")

// FlyAway Plugin
addSbtPlugin("org.flywaydb" % "flyway-sbt" % "3.2.1")
resolvers += "Flyway" at "http://flywaydb.org/repo"
