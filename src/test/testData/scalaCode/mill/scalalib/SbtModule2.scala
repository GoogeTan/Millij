package mill.scalalib

import scala.collection.immutable.Map

trait SbtModule2 extends mill.api.Module {
  def dict : Map[String, mill.api.TestStruct]
}
