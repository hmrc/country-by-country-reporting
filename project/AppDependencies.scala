import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {
  val mongoVersion = "1.2.0"

  val compile = Seq(
    "uk.gov.hmrc"       %% "bootstrap-backend-play-28" % "7.12.0",
    "org.julienrf"      %% "play-json-derived-codecs"  % "10.1.0",
    "com.lucidchart"    %% "xtract"                    % "2.3.0-alpha3",
    "uk.gov.hmrc.mongo" %% "hmrc-mongo-play-28"        % mongoVersion
  )

  val test = Seq(
    "org.scalatest"          %% "scalatest"               % "3.2.10",
    "org.scalatestplus"      %% "scalacheck-1-15"         % "3.2.10.0",
    "org.scalatestplus"      %% "mockito-3-4"             % "3.2.10.0",
    "org.scalatestplus.play" %% "scalatestplus-play"      % "5.1.0",
    "com.vladsch.flexmark"    % "flexmark-all"            % "0.64.0",
    "org.mockito"            %% "mockito-scala"           % "1.17.12",
    "wolfendale"             %% "scalacheck-gen-regexp"   % "0.1.2",
    "uk.gov.hmrc"            %% "bootstrap-test-play-28"  % "7.12.0",
    "uk.gov.hmrc.mongo"      %% "hmrc-mongo-test-play-28"  % mongoVersion,
    "com.github.tomakehurst"  % "wiremock-standalone"     % "2.23.2"
  ).map(_ % "test, it")
}
