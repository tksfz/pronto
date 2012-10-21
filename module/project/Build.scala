import sbt._
import Keys._
import PlayProject._

object ApplicationBuild extends Build {

    val appName         = "pronto"
    val appVersion      = "1.0-SNAPSHOT"

    val appDependencies = Seq(
      // Add your project dependencies here,
    )

    val main = PlayProject(appName, appVersion, appDependencies, mainLang = SCALA).settings(
      // Add your own project settings here      
      autoCompilerPlugins := true,
      addCompilerPlugin("org.scala-lang.plugins" % "continuations" % "2.9.1"),
      scalacOptions += "-P:continuations:enable"
    )

}
