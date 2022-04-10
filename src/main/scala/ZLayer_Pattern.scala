import zio.{ExitCode, Has, Task, ZIO, ZLayer}

// https://blog.rockthejvm.com/structuring-services-with-zio-zlayer/

case class User(name: String, email: String)

object UserEmailer {
  // service definition
  trait Service {
    def notify(u: User, msg: String): Task[Unit]
  }

  // layer; includes service implementation
  val live: ZLayer[Any, Nothing, Has[UserEmailer.Service]] = ZLayer.succeed(new Service {
    override def notify(u: User, msg: String): Task[Unit] =
      Task {
        println(s"[Email service] Sending $msg to ${u.email}")
      }
  })

  // front-facing API, aka "accessor"
  def notify(u: User, msg: String): ZIO[Has[UserEmailer.Service], Throwable, Unit] = ZIO.accessM(_.get.notify(u, msg))
}


object UserDb {
  // service definition
  trait Service {
    def insert(user: User): Task[Unit]
  }

  // layer - service implementation
  val live: ZLayer[Any, Nothing, Has[UserDb.Service]] = ZLayer.succeed {
    new Service {
      override def insert(user: User): Task[Unit] = Task {
        // can replace this with an actual DB SQL string
        println(s"[Database] insert into public.user values ('${user.name}')")
      }
    }
  }

  // accessor
  def insert(u: User): ZIO[Has[UserDb.Service], Throwable, Unit] = ZIO.accessM(_.get.insert(u))
}





object ZLayerPlayground extends zio.App {

  val userBackendLayer: ZLayer[Any, Nothing, Has[UserEmailer.Service] with Has[UserDb.Service]] =
    UserDb.live ++ UserEmailer.live

  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] =
    UserEmailer
      .notify(User("Daniel", "daniel@rockthejvm.com"), "Welcome to Rock the JVM!") // the specification of the action
      //.provideLayer(UserEmailer.live)
      .provideLayer(userBackendLayer) // plugging in a real layer/implementation to run on
      .exitCode // trigger the effect
}





object UserSubscription {

  // service definition
  class Service(notifier: UserEmailer.Service, userModel: UserDb.Service) {
    def subscribe(u: User): Task[User] = {
      for {
        _ <- userModel.insert(u)
        _ <- notifier.notify(u, s"Welcome, ${u.name}! Here are some ZIO articles for you here at Rock the JVM.")
      } yield u
    }
  }

  // layer with service implementation via dependency injection
  val live: ZLayer[Has[UserEmailer.Service] with Has[UserDb.Service], Nothing, Has[UserSubscription.Service]] =
    ZLayer.fromServices[UserEmailer.Service, UserDb.Service, UserSubscription.Service] { (emailer, db) =>
      new Service(emailer, db)
    }

  // accessor
  def subscribe(u: User): ZIO[Has[UserSubscription.Service], Throwable, User] = ZIO.accessM(_.get.subscribe(u))
}

object ZLayersPlaygroundCombo extends zio.App {
  override def run(args: List[String]): ZIO[zio.ZEnv, Nothing, ExitCode] = {
    val userRegistrationLayer = (UserDb.live ++ UserEmailer.live) >>> UserSubscription.live

    UserSubscription.subscribe(User("daniel", "daniel@rockthejvm.com"))
      .provideLayer(userRegistrationLayer)
      .catchAll(t => ZIO.succeed(t.printStackTrace()).map(_ => ExitCode.failure))
      .map { u =>
        println(s"Registered user: $u")
        ExitCode.success
      }
  }
}