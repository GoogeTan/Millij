package katze.millij.data

enum ScalaVersioning:
  case Java        // 1 colon (:)
  case ScalaBinary // 2 colons (::)
  case ScalaFull   // 3 colons (:::)

enum PlatformVersion:
  case Standard // 1 colon (:)
  case NativeJS // 2 colons (::)
end PlatformVersion

case class PartialMillDependency(
  groupId: String,
  scalaVersioning: Option[ScalaVersioning] = None,
  artifactId: Option[String] = None,
  platformVersion: Option[PlatformVersion] = None,
  version: Option[String] = None
):
  def getScalaBinaryVersion(fullVersion: String): String =
    if fullVersion.startsWith("3.") then
      "3"
    else
      fullVersion.split('.').take(2).mkString(".")
    end if
  end getScalaBinaryVersion

  def buildArtifactId(
    scalaVersion: String,
    platformSuffix: Option[String] = None
  ): Option[String] =
    artifactId.map:
      baseArtifact =>
        val platformPart = platformVersion match
          case Some(PlatformVersion.NativeJS) =>
            platformSuffix.map(p => s"_$p").getOrElse("")
          case _ =>
            ""

        val scalaPart = scalaVersioning match
          case Some(ScalaVersioning.ScalaBinary) =>
            s"_${getScalaBinaryVersion(scalaVersion)}"
          case Some(ScalaVersioning.ScalaFull) =>
            s"_$scalaVersion"
          case Some(ScalaVersioning.Java) | None =>
            ""

        s"$baseArtifact$platformPart$scalaPart"
  end buildArtifactId
end PartialMillDependency

object PartialMillDependency:
  def parse(input: String): PartialMillDependency =
    val scalaSepIdx = input.indexOf(':')

    if scalaSepIdx == -1 then
      return PartialMillDependency(groupId = input)

    val groupId = input.substring(0, scalaSepIdx)
    val afterGroupId = input.substring(scalaSepIdx)

    val scalaColons = afterGroupId.takeWhile(_ == ':').take(3)
    val scalaVersion = scalaColons.length match
      case 1 => ScalaVersioning.Java
      case 2 => ScalaVersioning.ScalaBinary
      case 3 => ScalaVersioning.ScalaFull
      case _ => ScalaVersioning.Java

    val afterScalaSep = afterGroupId.substring(scalaColons.length)

    if afterScalaSep.isEmpty then
      return PartialMillDependency(
        groupId = groupId,
        scalaVersioning = Some(scalaVersion)
      )

    val platformSepIdx = afterScalaSep.indexOf(':')

    if platformSepIdx == -1 then
      return PartialMillDependency(
        groupId = groupId,
        scalaVersioning = Some(scalaVersion),
        artifactId = Some(afterScalaSep)
      )

    val artifactId = afterScalaSep.substring(0, platformSepIdx)
    val afterArtifactId = afterScalaSep.substring(platformSepIdx)

    val platformColons = afterArtifactId.takeWhile(_ == ':').take(2)
    val platformVersion = platformColons.length match
      case 1 => PlatformVersion.Standard
      case 2 => PlatformVersion.NativeJS
      case _ => PlatformVersion.Standard

    val versionStr = afterArtifactId.substring(platformColons.length)

    val version = if versionStr.isEmpty then None else Some(versionStr)

    PartialMillDependency(
      groupId = groupId,
      scalaVersioning = Some(scalaVersion),
      artifactId = Some(artifactId),
      platformVersion = Some(platformVersion),
      version = version
    )
  end parse
end PartialMillDependency
