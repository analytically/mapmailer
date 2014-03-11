name := "mapmailer"

version := "1.0-SNAPSHOT"

resolvers += "Typesafe repository" at "http://repo.typesafe.com/typesafe/releases/"

resolvers += "Open Source Geospatial Foundation Repository" at "http://download.osgeo.org/webdav/geotools/"

play.Project.playScalaSettings

libraryDependencies ++= Seq(
  cache,
  filters
)

libraryDependencies += "org.reactivemongo" %% "play2-reactivemongo" % "0.10.2" excludeAll ExclusionRule(organization = "org.apache.logging.log4j")

libraryDependencies += "org.apache.logging.log4j" % "log4j-to-slf4j" % "2.0-rc1"

libraryDependencies += "uk.co.coen" % "capsulecrm-java" % "1.2.3"

libraryDependencies += "org.apache.camel" % "camel-core" % "2.11.4"

libraryDependencies += "org.apache.camel" % "camel-csv" % "2.11.4"

libraryDependencies += "org.apache.camel" % "camel-bindy" % "2.11.4"

libraryDependencies += "org.geotools" % "gt-main" % "10.5" excludeAll ExclusionRule(organization = "javax.media")

libraryDependencies += "org.geotools" % "gt-epsg-hsql" % "10.5" excludeAll ExclusionRule(organization = "javax.media")


libraryDependencies ~= { _ map {
  case m if m.organization == "org.apache.httpcomponents" =>
    m.exclude("commons-logging", "commons-logging")
  case m if m.organization == "com.typesafe.play" =>
    m.exclude("commons-logging", "commons-logging").exclude("org.springframework", "spring-aop").exclude("org.springframework", "spring-context").exclude("org.springframework", "spring-beans").exclude("org.springframework", "spring-core").exclude("org.springframework", "spring-expression").exclude("org.springframework", "spring-asm")
  case m => m
}}
