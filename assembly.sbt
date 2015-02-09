import sbtassembly.AssemblyPlugin.autoImport._

assemblyJarName in assembly := "mapmailer.jar"

assemblyMergeStrategy in assembly <<= (mergeStrategy in assembly) { (wrapped) => {
    case PathList("play", "core", xs @ _*) => MergeStrategy.first
    case PathList("org", "xmlpull", xs @ _*) => MergeStrategy.first
    case x => wrapped(x)
  }
}

mainClass in assembly := Some("play.core.server.NettyServer")

fullClasspath in assembly += Attributed.blank(PlayKeys.playPackageAssets.value)
