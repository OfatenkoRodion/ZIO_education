import zio._

object FibersTutorial extends zio.App {

  def printThread = s"[${Thread.currentThread().getName}]"

  val bathTime = ZIO.succeed("Going to the bathroom")
  val boilingWater = ZIO.succeed("Boiling some water")
  val preparingCoffee = ZIO.succeed("Preparing the coffee")

  def sequentialWakeUpRoutine(): ZIO[Any, Nothing, Unit] = for {
    _ <- bathTime.debug(printThread).fork
    _ <- boilingWater.debug(printThread)
    _ <- preparingCoffee.debug(printThread)
  } yield ()

  def concurrentWakeUpRoutine(): ZIO[Any, Nothing, Unit] = for {
    bathFiber <- bathTime.debug(printThread).fork
    boilingFiber <- boilingWater.debug(printThread).fork
    zippedFiber = bathFiber.zip(boilingFiber)
    result <- zippedFiber.join.debug(printThread)
    // *> operator, which is an alias for the zipRight function, and let us concatenate the execution of two effects not depending on each other.
    _ <- ZIO.succeed(s"$result...done").debug(printThread) *> preparingCoffee.debug(printThread)
  } yield ()

  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] =
    concurrentWakeUpRoutine().exitCode
}