import sbt._

class ChuteProject(info: ProjectInfo) extends DefaultProject(info) {
  // --- Repositories ------------------------------------------------------------------------------
  val localMavenRepo = "Local Maven Repository" at "file://"+Path.userHome+"/.m2/repository"
  val jbossRepo = "JBOSS Repository" at "http://repository.jboss.org/maven2"
  val mavenCentralRepo = "Maven Main Repository" at "http://repo1.maven.org/maven2"

  // --- Dependencies ------------------------------------------------------------------------------
	val netty = "org.jboss.netty" % "netty" % "3.1.0.CR1"
  val dispatchJSON = "net.databinder" %% "dispatch-json" % "0.5.1"

  // Plugin in if using java 5, may require changes to source
  // val jsr166x = "jsr166" % "jsr166" % "x" from
  //     "http://gee.cs.oswego.edu/dl/jsr166/dist/jsr166x.jar"

  // --- Test Dependencies -------------------------------------------------------------------------
  // Specs maven repo version isnt compatible with scala 2.7.5 therefore we have to 
  // resolve all deps manually here... *sigh*
  // val specs = "org.scala-tools.testing" % "specs" % "1.5.0" % "test->default"
  val scalacheck = "org.scala-tools.testing" % "scalacheck" % "1.5" % "test->default"
  val scalatest = "org.scala-tools.testing" % "scalatest" % "0.9.5" % "test->default"
  val junit = "junit" % "junit" % "4.5" % "test->default"
  val cglib = "cglib" % "cglib" % "2.1_3" % "test->default"
  val asm = "asm" % "asm" % "1.5.3" % "test->default"
  val hamcrest = "org.hamcrest" % "hamcrest-all" % "1.1" % "test->default"
  val jmock = "org.jmock" % "jmock-junit4" % "2.4.0" % "test->default"
  val mockito = "org.mockito" % "mockito-all" % "1.7" % "test->default"

	// --- General Behaviour and Options -------------------------------------------------------------
	override def compileOptions = (
    // CompileOption("-Xlog-implicits") :: 
    CompileOption("-Xcheckinit") :: 
    super.compileOptions.toList)

  // --- Shortcuts ---------------------------------------------------------------------------------
  lazy val c = compileAction
  lazy val t = testAction
  lazy val tc = testCompileAction
  lazy val to = testOnlyAction
  lazy val tq = testQuickAction
  lazy val tf = testFailedAction
  lazy val s = consoleAction
  lazy val p = packageAction
}

// vim: set tabstop=2 shiftwidth=2 et:
