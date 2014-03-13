import AssemblyKeys._

assemblySettings

jarName in assembly := "mapmailer.jar"

mergeStrategy in assembly <<= (mergeStrategy in assembly) { (wrapped) => {
    case PathList("play", "core", xs @ _*) => MergeStrategy.first
    case PathList("org", "xmlpull", xs @ _*) => MergeStrategy.first
    case x => wrapped(x)
  }
}

mainClass in assembly := Some("play.core.server.NettyServer")