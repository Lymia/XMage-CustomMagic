val mageVersion = "1.4.15"

val settings = Seq(
  organization := "moe.lymia",

  version := "1.0",
  scalaVersion := "2.11.8",

  resolvers += Resolver.mavenLocal,
  libraryDependencies += "org.mage" % "mage" % mageVersion,
  libraryDependencies += "org.mage" % "mage-server" % mageVersion,

  resolvers += Resolver.sonatypeRepo("releases"),
  addCompilerPlugin("org.scalamacros" % "paradise" % "2.1.0" cross CrossVersion.full),
  libraryDependencies += "org.scala-lang" % "scala-reflect" % scalaVersion.value
)

val xmage = (project in file("modules/xmage")).settings(settings : _*).settings(
  name := "xmage-scala-wrapper"
)
val custommagic = (project in file("modules/custommagic")).settings(settings : _*).settings(
  name := "xmage-custommagic"
).dependsOn(xmage)