package katze.millij.data

import cats.{Eq, Show}

/**
 * An opaque type representing a single, valid Scala 3 identifier.
 */
opaque type ScalaIdentifier = String

object ScalaIdentifier:
  // 1. Regex patterns for different identifier categories
  // Matches alphanumeric strings (including Unicode letters)
  private val AlphaNum = """^[\p{L}_][\p{L}\p{Nd}_]*$""".r
  // Matches valid ASCII operator characters (including *)
  private val Operator = """^[!#%&*+\-/:<=>?@\\^|~]+$""".r
  // Matches mixed identifiers like `foo_+`
  private val Mixed    = """^[\p{L}_][\p{L}\p{Nd}_]*_[!#%&*+\-/:<=>?@\\^|~]+$""".r
  // Matches any string enclosed in backticks
  private val Backtick = """^`[^`]+`$""".r

  // 2. Scala 3 reserved words and symbols that cannot be bare identifiers
  private val ReservedWords = Set(
    "abstract", "case", "catch", "class", "def", "do", "else", "enum",
    "export", "extends", "false", "final", "finally", "for", "given",
    "if", "implicit", "import", "lazy", "match", "new", "null", "object",
    "override", "package", "private", "protected", "return", "sealed",
    "super", "then", "throw", "trait", "true", "try", "type", "val",
    "var", "while", "with", "yield",
    "=", "=>", "<-", "<:", "<%", ">:", "#", "@", ":"
  )

  /**
   * Smart constructor: Safely attempts to parse a string into a ScalaIdentifier.
   */
  def fromStringEither(s: String): Either[String, ScalaIdentifier] =
    if s == null || s.trim.isEmpty then
      Left("Identifier cannot be null or empty")
    else if Backtick.matches(s) then
      Right(s) // Backticked identifiers bypass keyword checks
    else if AlphaNum.matches(s) || Operator.matches(s) || Mixed.matches(s) then
      if ReservedWords.contains(s) then
        Left(s"'$s' is a reserved keyword in Scala 3 and must be enclosed in backticks")
      else
        Right(s)
    else
      Left(s"'$s' contains invalid characters for a Scala identifier")
    end if
  end fromStringEither
  
  def fromStringOption(s : String) : Option[ScalaIdentifier] =
    fromStringEither(s).toOption
  end fromStringOption

  /**
   * Unsafe constructor for hardcoded strings you know are valid.
   * Throws an exception if invalid.
   */
  def unsafe(s: String): ScalaIdentifier =
    fromStringEither(s) match
      case Right(id) => id
      case Left(err) => throw new IllegalArgumentException(err)

  extension (id: ScalaIdentifier)
    def asString: String = id
    def length = id.length

  given Show[ScalaIdentifier] = Show.show(identity)
  
  given Eq[ScalaIdentifier] = summon
end ScalaIdentifier