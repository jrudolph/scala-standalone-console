import sbt._

class ScalaStandAloneConsole(info: ProjectInfo) extends DefaultProject(info) with ProguardProject {
  override val mainClass = Some("virtualvoid.scala.ScalaConsoleLauncher")

  val compiler = "org.scala-lang" % "scala-compiler" % "2.8.0"

  override def proguardOptions = List(proguardKeepMain("virtualvoid.scala.ScalaConsoleLauncher"), "-keep class com.sun.jna.**", "-dontoptimize", "-dontshrink")
  //override def packageOptions = ManifestAttributes("Classpath", "jna.jar") :: super.packageOptions
  //val jna = "net.java.dev.jna" % "jna" % "3.2.3"
}
