object Docs extends App {

  /** ZIO[-R, +E, +A]
   *    an input type R, also known as environment
   *    an error type E, which can be anything (not necessarily a Throwable)
   *    a value type A
   *
   * Conceptually, a ZIO instance is equivalent to a function R => Either[E,A]
   * */

  /** ZLayer has 3 type arguments:
   *
   *   an input type RIn, aka “dependency” type
   *   an error type E, for the error that might arise during creation of the service
   *   an output type ROut
   */
}