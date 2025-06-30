import uk.gov.hmrc.DefaultBuildSettings

ThisBuild / majorVersion := 0
ThisBuild / scalaVersion := "2.13.16"

lazy val microservice = Project("country-by-country-reporting", file("."))
  .enablePlugins(PlayScala, SbtDistributablesPlugin)
  .settings(
    PlayKeys.playDefaultPort := 10022,
    libraryDependencies ++= AppDependencies.compile ++ AppDependencies.test,
    Compile / scalafmtOnCompile                                := true,
    Test / scalafmtOnCompile                                   := true,
    ThisBuild / scalafmtOnCompile.withRank(KeyRanks.Invisible) := true,
    scalacOptions ++= Seq(
      "-Wconf:src=routes/.*:s",
      "-Wconf:src=.+/test/.+:s",
      "-Wconf:cat=deprecation&msg=\\.*()\\.*:s",
      "-Wconf:cat=unused-imports&site=<empty>:s",
      "-Wconf:cat=unused&src=.*RoutesPrefix\\.scala:s",
      "-Wconf:cat=unused&src=.*Routes\\.scala:s"
    )
  )
  .settings(inConfig(Test)(testSettings): _*)
  .settings(scoverageSettings)
  .settings(resolvers += Resolver.mavenCentral)
  .settings(CodeCoverageSettings.settings: _*)
  .settings(
    ThisBuild / libraryDependencySchemes ++= Seq(
      "org.scala-lang.modules" %% "scala-xml" % VersionScheme.Always
    )
  )
  .disablePlugins(JUnitXmlReportPlugin) //Required to prevent https://github.com/scalatest/scalatest/issues/1427

lazy val scoverageSettings = {
  import scoverage.ScoverageKeys

  val excludedPackages = Seq(
    "<empty>",
    ".*Reverse.*",
    ".*Routes.*",
    ".*standardError*.*",
    ".*main_template*.*",
    "uk.gov.hmrc.BuildInfo",
    "app.*",
    "prod.*",
    "config.*",
    "testOnlyDoNotUseInAppConf.*",
    "views.html.*",
    "testOnly.*",
    ".*.metrics.*",
    ".*.audit.*",
    ".*javascript.*",
    ".*GuiceInjector;",
    ".*ControllerConfiguration",
    ".*LanguageSwitchController",
    ".*handlers.*",
    ".*utils.*",
    ".*Repository.*"
  )

  Seq(
    ScoverageKeys.coverageExcludedFiles    := excludedPackages.mkString(";"),
    ScoverageKeys.coverageMinimumStmtTotal := 80,
    ScoverageKeys.coverageFailOnMinimum    := true,
    ScoverageKeys.coverageHighlighting     := true
  )
}

lazy val testSettings: Seq[Def.Setting[_]] = Seq(fork := true, unmanagedSourceDirectories += baseDirectory.value / "test-common")

lazy val it = project
  .enablePlugins(PlayScala)
  .dependsOn(microservice % "test->test")
  .settings(DefaultBuildSettings.itSettings())
  .settings(libraryDependencies ++= AppDependencies.itDependencies)
