name := """sonic"""

version := "1.0.0"

lazy val root = (project in file(".")).enablePlugins(PlayScala)

scalaVersion := "2.11.12"

libraryDependencies ++= Seq(
  cache,
  ws,
  filters,
  specs2 % Test,
   "commons-io" % "commons-io" % "2.6"
)

libraryDependencies += "org.apache.poi" % "poi" % "4.1.0"
libraryDependencies += "org.apache.poi" % "poi-ooxml" % "4.1.0"
libraryDependencies += "org.apache.poi" % "poi-ooxml-schemas" % "4.1.0"
libraryDependencies += "org.apache.poi" % "poi-scratchpad" % "4.1.0"

libraryDependencies += "io.vertx" % "vertx-core" % "3.9.3"
libraryDependencies += "com.github.wendykierp" % "JTransforms" % "3.1"
libraryDependencies += "io.vertx" % "vertx-rx-java2" % "3.9.3"


libraryDependencies += "org.scream3r" % "jssc" % "2.8.0"
libraryDependencies += "com.github.nscala-time" %% "nscala-time" % "2.22.0"
libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "2.7.0"


mappings in Universal ++=
(baseDirectory.value / "report_template" * "*" get) map
    (x => x -> ("report_template/" + x.getName))

mappings in Universal ++=
(baseDirectory.value / "export/" * "*" get) map
    (x => x -> ("export/" + x.getName))

resolvers += "scalaz-bintray" at "http://dl.bintray.com/scalaz/releases"

// Play provides two styles of routers, one expects its actions to be injected, the
// other, legacy style, accesses its actions statically.
//routesGenerator := InjectedRoutesGenerator

scalacOptions ++= Seq("-feature")

fork in run := false