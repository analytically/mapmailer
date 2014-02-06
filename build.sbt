name := "mapmailer"

version := "1.0-SNAPSHOT"

resolvers += "Open Source Geospatial Foundation Repository" at "http://download.osgeo.org/webdav/geotools/"

libraryDependencies ++= Seq(
  jdbc,
  anorm,
  cache
)

libraryDependencies += "com.google.guava" % "guava" % "15.0"

libraryDependencies +=  "com.google.code.findbugs" % "jsr305" % "2.0.3" // see http://stackoverflow.com/questions/10007994/why-do-i-need-jsr305-to-use-guava-in-scala

libraryDependencies += "org.reactivemongo" % "play2-reactivemongo_2.10" % "0.10.2"

libraryDependencies += "com.typesafe.play.extras" %% "play-geojson" % "1.0.0"

libraryDependencies += "uk.co.coen" % "capsulecrm-java" % "1.2.2"

libraryDependencies += "org.apache.camel" % "camel-core" % "2.11.3"

libraryDependencies += "org.apache.camel" % "camel-csv" % "2.11.3"

libraryDependencies += "org.apache.camel" % "camel-bindy" % "2.11.3"

libraryDependencies += "org.apache.camel" % "camel-jackson" % "2.11.3"

libraryDependencies += "org.apache.camel" % "camel-http" % "2.11.3"

libraryDependencies += "org.geotools" % "gt-main" % "10.3" excludeAll ExclusionRule(organization = "javax.media")

libraryDependencies += "org.geotools" % "gt-epsg-hsql" % "10.3" excludeAll ExclusionRule(organization = "javax.media")

play.Project.playScalaSettings
