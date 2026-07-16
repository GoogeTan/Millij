package mill.scalalib

trait ScalaModule extends mill.api.Module {
  def scalaVersion: String
  def mvnDeps: Seq[mill.javalib.Dep]
  def someField: mill.javalib.Dep
  def seqMethod: Seq[mill.api.TestStruct]
  def structMethods: mill.api.NestedStruct
}
