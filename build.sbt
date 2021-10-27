import org.scalastyle.sbt.ScalastylePlugin

import scala.util.Properties

scalaVersion := "2.11.12"

scalacOptions := Seq("-unchecked", "-deprecation", "-encoding", "utf8")

val applicationDependencies = {
  val akkaVersion           = "2.5.23"
  val akkaHttpVersion       = "10.0.9"
  val akkaHttpJson4sVersion = "1.17.0"
  val scalaTestVersion      = "3.0.1"
  val h2Version             = "1.4.192"
  val specs2Version         = "4.7.0"
  val json4sVersion         = "3.6.7"
  val httpClientVersion     = "1.2.6"
  val slf4sVersion          = "1.7.12"
  val logbackVersion        = "1.2.1"
  val nScalaTimeVersion     = "2.22.0"
  val jodaTimeVersion       = "2.9.4"
  val jodaConvertVersion    = "1.8"
  val slickVersion          = "3.1.1"
  val monitoringVersion     = "1.1.0"
  val postgresVersion       = "9.4-1206-jdbc41"
  val WireMockVersion       = "2.6.0"
  val awsVersion            = "1.11.269"

  Seq(
    "org.postgresql"          %  "postgresql"         % postgresVersion,
    "com.typesafe.slick"      %% "slick"              % slickVersion,
    "com.typesafe.slick"      %% "slick-hikaricp"     % slickVersion,
    "org.slf4s"               %% "slf4s-api"          % slf4sVersion,
    "org.json4s"              %% "json4s-jackson"     % json4sVersion,
    "org.json4s"              %% "json4s-ext"         % json4sVersion,
    "org.json4s"              %% "json4s-native"      % json4sVersion,
    "ch.qos.logback"          %  "logback-classic"    % logbackVersion,
    "com.github.nscala-time"  %% "nscala-time"        % nScalaTimeVersion,
    "com.typesafe.akka"       %% "akka-actor"         % akkaVersion,
    "com.typesafe.akka"       %% "akka-stream"        % akkaVersion,
    "com.typesafe.akka"       %% "akka-http"          % akkaHttpVersion,
    "com.typesafe.akka"       %% "akka-http-testkit"  % akkaHttpVersion,
    "de.heikoseeberger"       %% "akka-http-json4s"   % akkaHttpJson4sVersion,
    "org.json4s"              %% "json4s-jackson"     % json4sVersion,
    "org.json4s"              %% "json4s-ext"         % json4sVersion,
    "joda-time"               %  "joda-time"          % jodaTimeVersion,
    "org.joda"                %  "joda-convert"       % jodaConvertVersion,
    "com.amazonaws"           %  "aws-java-sdk-sns"   % awsVersion,
    "org.specs2"              %% "specs2-core"        % specs2Version    % "it,test",
    "org.specs2"              %% "specs2-mock"        % specs2Version    % "it,test",
    "com.h2database"          %  "h2"                 % h2Version        % "it,test",
    "org.scalatest"           %% "scalatest"          % scalaTestVersion % "it,test",
    "com.github.tomakehurst"  %  "wiremock"           % WireMockVersion  % "it,test"
  )
}

val assemblySettings = Seq(
  test in assembly := {},
  assemblyJarName in assembly := s"${name.value}.jar",
  assemblyMergeStrategy in assembly := ServiceAssembly.aspectjAopMergeStrategy
)


val speculateSettings = Seq(
  speculateServiceName := name.value,
  speculateVersion := Properties.envOrElse("BUILD_NUMBER", "dev"),
  speculateRequires := Seq(
    "java-1.8.0-openjdk",
    "rms-collectd",
    "rms-filebeat",
    "ibl-sysadmin",
    "rms-aspectj-weaver",
    "cosmos-ca-chains"),
  speculateBuildRequires := Seq("java-1.8.0-openjdk"),
  speculateJavaOpts := Seq(
    "-server",
    "-javaagent:/usr/lib/rms-aspectj-weaver/aspectjweaver-1.8.9.jar"
  )
)

val `test-all` = taskKey[Unit]("Run Unit tests, integration tests and scalastyle.")
val testSettings = IntegrationTests.settings ++ Seq(
  `test-all` in Compile := Def.sequential(
    test in Test,
    test in IntegrationTests.ItTest,
    (ScalastylePlugin.scalastyle in Compile).toTask("")
  ).value
)

lazy val `the-real-tuesday-weld` = (project in file("."))
  .enablePlugins(SbtSpeculate)
  .configs(IntegrationTests.ItTest)
  .settings(
    organization := "",
    name := "rms-the-real-tuesday-weld",
    libraryDependencies ++= applicationDependencies,
    resolvers ++= Seq("Artifactory" at "https://artifactory/repo/"),
    javaOptions in run ++= Seq(
      "-Dconfig.resource=application.dev.conf",
      "-Dlogback.configurationFile=logback.dev.xml"
    ),
    fork in run := true
  )
  .settings(testSettings)
  .settings(assemblySettings)
  .settings(speculateSettings)
  .enablePlugins(SbtTrace).settings(Seq(traceUser := ""))
