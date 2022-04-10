import CreateId.IdService
import zio.{ExitCode, Has, Task, URIO, ZIO, ZLayer}

import java.util.UUID

object CreateId {

  trait IdService {
    def generate: Task[UUID]
  }
  val live: ZLayer[Any, Nothing, Has[IdService]] = ZLayer.succeed(new IdService {
    override def generate: Task[UUID] =
      Task {
        UUID.randomUUID()
      }
  })
  def generate: ZIO[Has[CreateId.IdService], Throwable, UUID] = ZIO.accessM(_.get.generate)
}

object CreateUser{
  case class User(id: UUID, name: String, age: Int)

  class Service(idService: IdService) {
    def create(name: String, age: Int): Task[User] = {
      for {
        id <- idService.generate
      } yield User(id, name, age)
    }
  }
  val live: ZLayer[Has[CreateId.IdService], Nothing, Has[CreateUser.Service]] =
    ZLayer.fromService[CreateId.IdService, CreateUser.Service] { idService =>
      new CreateUser.Service(idService)
    }

  def create(name: String, age: Int): ZIO[Has[CreateUser.Service], Throwable, User] = ZIO.accessM(_.get.create(name, age))
}

object IdMain extends zio.App {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    CreateId.generate
      .provideLayer(CreateId.live)
      .catchAll(t => ZIO.succeed(t.printStackTrace()).map(_ => ExitCode.failure))
      .map { id =>
        println(s"id: $id")
        ExitCode.success
      }
  }
}

object UserMain extends zio.App {
  override def run(args: List[String]): URIO[zio.ZEnv, ExitCode] = {
    val layer = (CreateId.live) >>> CreateUser.live

    CreateUser.create("John", 25)
      .provideLayer(layer)
      .catchAll(t => ZIO.succeed(t.printStackTrace()).map(_ => ExitCode.failure))
      .map { user =>
        println(s"user: $user")
        ExitCode.success
      }
  }
}
