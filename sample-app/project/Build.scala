import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "sample-app"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      "org.tksfz" %% "pronto" % "0.1"
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      resolvers += "tksfz releases" at "http://tksfz.github.com/mvn-repo/releases/",
      autoCompilerPlugins := true,
      addCompilerPlugin("org.scala-lang.plugins" % "continuations" % "2.9.1"),
      scalacOptions += "-P:continuations:enable"
    )

}
