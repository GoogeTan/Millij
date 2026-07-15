package katze.millij.place

enum TestPlace:
  case ClassFromName(names : List[String])
  case MemberOf(original : TestPlace, field : String)
  case UnSeqOf(original : TestPlace)
end TestPlace
