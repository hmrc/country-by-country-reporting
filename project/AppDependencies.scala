import sbt._

object AppDependencies {
  private val mongoVersion     = "2.6.0"
  private val bootstrapVersion = "9.13.0"

  val compile: Seq[ModuleID] = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-30" % bootstrapVersion,
    "org.julienrf"      %% "play-json-derived-codecs"  % "10.1.0",
    "com.lucidchart"    %% "xtract"                    % "2.3.0",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-30"        % mongoVersion
  )

  val test: Seq[ModuleID] = Seq(
    "org.scalatest"          %% "scalatest"               % "3.2.10",
    "org.scalatestplus"      %% "scalacheck-1-15"         % "3.2.11.0",
    "org.scalatestplus"      %% "mockito-3-4"             % "3.2.10.0",
    "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0",
    "com.vladsch.flexmark"    % "flexmark-all"            % "0.64.0",
    "org.mockito"            %% "mockito-scala"           % "1.17.37",
    "wolfendale"             %% "scalacheck-gen-regexp"   % "0.1.2",
    "uk.gov.hmrc"            %% "bootstrap-test-play-30"  % bootstrapVersion,
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-30" % mongoVersion,
  ).map(_ % Test)

  val itDependencies: Seq[ModuleID] = Seq(
    "uk.gov.hmrc" %% "bootstrap-test-play-30" % bootstrapVersion % Test
  )
}
