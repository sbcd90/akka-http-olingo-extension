name := "akka-http-olingo-extension"
organization := "org.apache.olingo"

version := "1.0"

scalaVersion := "2.11.8"

libraryDependencies ++= Seq ("com.typesafe.akka" %% "akka-http-experimental" % "2.4.2",
                          "org.apache.olingo" % "odata-server-api" % "4.1.0",
                          "org.apache.olingo" % "odata-server-core" % "4.1.0",
                          "javax" % "javaee-web-api" % "7.0",
                          "org.apache.commons" % "commons-io" % "1.3.2"
                        )
    