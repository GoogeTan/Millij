package mill.scalalib

trait Dep1 {
 type A
}

trait Dep2 extends Dep1 {
 trait Dep3 {
   def foo : A
 }
}

trait Dep4 extends Dep1 {
 override type A = mill.api.TestStruct
}
