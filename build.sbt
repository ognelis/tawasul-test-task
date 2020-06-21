
lazy val mtproto = (project in file("."))
  .settings(
    name := "mtproto",
    scalaVersion := "2.13.2",
    scalacOptions ++= Seq(
      "-encoding", "utf8",
      "-language:implicitConversions",
      "-language:higherKinds",
    ),
    addCompilerPlugin("org.typelevel" % "kind-projector_2.13.2" % "0.11.0"),
    libraryDependencies ++= Dependencies.mtproto,
  )
