import zio.{IO, UIO, ZIO}

object ReadWriteExample1 {

  // data structures to wrap a value or an error
  // the input type is "any", since they don't require any input
  val success: UIO[Int] = ZIO.succeed(42)
  val fail: IO[String, Nothing] = ZIO.fail("Something went wrong") // notice the error can be of any type

  // reading and writing to the console are effects
  // the input type is a Console instance, which ZIO provides with the import
  import zio.console._
  val greetingZio =
    for {
      _    <- putStrLn("Hi! What is your name?")
      name <- getStrLn
      _    <- putStrLn(s"Hello, $name!")
    } yield ()
}

object ZioPlayground extends zio.App {
  def run(args: List[String]) =
    ReadWriteExample1.greetingZio.exitCode
}