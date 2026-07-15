package katze.millij

import org.junit.Assert.fail

def assertMatches[A, B](value: A, matcher: A => Option[B], text: String = "Failed to match"): B =
  matcher(value) match
    case Some(value) =>
      value
    case None =>
      fail(text)
      ???
end assertMatches
