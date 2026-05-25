package katze.millij

enum TestType:
  case ClassFromName(names : List[String])
  case MemberOf(original : TestType, field : String)
  case UnSeqOf(original : TestType)
end TestType
