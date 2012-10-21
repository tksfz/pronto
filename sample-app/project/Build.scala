import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "sample-app"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "org.tksfz" %% "pronto" % "0.1-SNAPSHOT"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      resolvers += "Local Play Repo" at "file://home/thom/play-2.0.3/repository/local"
    )

}
