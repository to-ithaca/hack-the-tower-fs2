package workshop

import fs2._
import fs2.util.syntax._
import fs2.io._
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousChannelGroup
import java.util.concurrent.Executors

import fs2.Task
import fs2.io.tcp.Socket
import fs2.util.{Async, Attempt}

trait Server extends App {
  lazy val port = 8080
  lazy val executor = Executors.newSingleThreadExecutor()
  implicit val group: AsynchronousChannelGroup = AsynchronousChannelGroup.withThreadPool(executor)
  implicit val strategy: Strategy = Strategy.fromExecutor(executor)

  def listen[F[_] : Async](f: Socket[F] => Stream[F, Unit]): Stream[F, Unit] = for {
    listen <- tcp.server[F](new InetSocketAddress("localhost", port))
    connection <- listen
    _ <- f(connection)
  } yield ()

  def logResult[A](result: Attempt[A]): Unit = result match {
    case Left(t) => println(s"error with value $t")
    case Right(_) => println("finished successfully")
  }
}

object EchoServer extends Server {
  def echo[F[_]](socket: Socket[F])(implicit F: Async[F]): Stream[F, Unit] = {
    val echo = for {
      bytes <- socket.read(1024)
      _ <- bytes match {
        case Some(chunk) => socket.write(chunk)
        case None => F.pure(())
      }
    } yield ()
    Stream.repeatEval(echo)
  }

  listen[Task](echo)
    .run
    .unsafeRunAsync(logResult)

}

object CountServer extends Server {

  def show(count: Int): Chunk[Byte] = {
    Chunk.bytes(s"$count\n".toCharArray.map(_.toByte))
  }

  def count[F[_]](socket: Socket[F])(implicit F: Async[F]): Stream[F, Unit] = {
    def each(value: Int): F[Int] = socket.read(1024).flatMap {
      case Some(chunk) =>
        val current = chunk.toBytes.size + value
        for {
          _ <- socket.write(show(current))
          next <- each(current)
        } yield next
      case None => each(value)
    }
    Stream.iterateEval(0)(each).map(_ => ())
  }

  listen[Task](count)
    .run
    .unsafeRunAsync(logResult)
}


