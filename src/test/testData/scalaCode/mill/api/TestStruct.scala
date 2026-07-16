package mill.api

final case class TestStruct(val a : String, val b : String, val c : String)
final case class NestedStruct(val a : String, val b : String, val c : TestStruct)
