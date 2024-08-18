scalaVersion := "2.13.5"
organization := "world.ultravanilla"
version := "1.51"
name := "signage"
resolvers ++= List(
    "jitpack.io" at "https://jitpack.io",
    "papermc" at "https://papermc.io/repo/repository/maven-public/",
    "dmulloy2-repo" at "https://repo.dmulloy2.net/nexus/repository/public/",
    "jcenter" at "https://jcenter.bintray.com",
    "m2-dv8tion" at "https://m2.dv8tion.net/releases",
)
libraryDependencies ++= List(
    "io.papermc.paper" % "paper-api" % "1.20-R0.1-SNAPSHOT" % Provided,
)

scalacOptions += "-target:17"
javacOptions ++= Seq("-source", "17", "-target", "17")

Compile / resourceGenerators += Def.task {
    val file = (Compile / resourceManaged).value / "plugin.yml"
    val pluginConfig = scala.io.Source.fromFile("plugin.yml").getLines.mkString("\n")
    val contents = pluginConfig.replace("@VERSION@", version.value)
    IO.write(file, contents)
    Seq(file)
}.taskValue

assembly / assemblyMergeStrategy := {
    case "module-info.class" => MergeStrategy.first
    case x =>
        val oldStrategy = (assembly / assemblyMergeStrategy).value
        oldStrategy(x)
}

