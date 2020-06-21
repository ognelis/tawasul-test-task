package ae.tawasul.mtproto.components.server

import cats.effect.Concurrent
import fs2.Stream
import fs2.concurrent.Queue
import fs2.io.tcp.Socket
import scodec.stream.{StreamDecoder, StreamEncoder}
import scodec.{Decoder, Encoder}

trait MessageSocket[F[_], In, Out] {
  def read: Stream[F, In]
  def write1(out: Out): F[Unit]
}

object MessageSocket {

  def apply[F[_]: Concurrent, In: Decoder, Out: Encoder](
    socket: Socket[F],
    outputBound: Int = 100,
    readMaxBytes: Int = 4096,
  ): F[MessageSocket[F, In, Out]] = {
    import cats.syntax.functor._
    Queue.bounded[F, Out](outputBound).map { outgoing =>
      new MessageSocket[F, In, Out] {
        def read: Stream[F, In] = {
          val readSocket = socket
            .reads(readMaxBytes)
            .through(StreamDecoder.many(Decoder[In]).toPipeByte[F])

          val writeOutput = outgoing.dequeue
            .through(StreamEncoder.many(Encoder[Out]).toPipeByte)
            .through(socket.writes(None))

          readSocket.concurrently(writeOutput)
        }

        def write1(out: Out): F[Unit] = outgoing.enqueue1(out)
      }
    }
  }
}
