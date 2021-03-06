import play.core.PlayVersion
import play.sbt.PlayImport._
import sbt.Keys.libraryDependencies
import sbt._

object AppDependencies {

  val compile = Seq(
    "uk.gov.hmrc"             %% "bootstrap-backend-play-28"  % "5.24.0",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-play-28"         % "0.63.0"
  )

  val test = Seq(
    "org.scalatestplus"       %% "scalatestplus-scalacheck"   % "3.1.0.0-RC2"       % Test,
    "wolfendale"              %% "scalacheck-gen-regexp"      % "0.1.2"             % Test,
    "uk.gov.hmrc"             %% "bootstrap-test-play-28"     % "5.24.0"            % "test, it",
    "uk.gov.hmrc.mongo"       %% "hmrc-mongo-test-play-28"    % "0.63.0"            % Test,
    "org.mockito"             %% "mockito-scala"              % "1.10.6"            % Test,
    "org.scalatest"           %% "scalatest"                  % "3.1.0"             % Test,
    "org.scalatestplus.play"  %% "scalatestplus-play"         % "5.1.0"             % Test,
    "com.github.tomakehurst"  % "wiremock-standalone"         % "2.23.2"            % Test,
    "com.vladsch.flexmark"    %  "flexmark-all"               % "0.36.8"            % "test, it"
  )
}
