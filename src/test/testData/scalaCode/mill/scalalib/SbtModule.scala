package mill.scalalib

import mill.api._

trait SbtModule extends ScalaModule {
  trait SbtTests {
     def something : String
  }
  trait SbtTests2
  def somethingWithTask : Task[String] 
}
