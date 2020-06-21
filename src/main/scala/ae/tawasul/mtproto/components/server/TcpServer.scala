package ae.tawasul.mtproto.components.server

import java.net.InetSocketAddress

import ae.tawasul.mtproto.components.protocol.mtProtoTransport.Message
import ae.tawasul.mtproto.components.protocol.rpc.ServerCommand.ResPQ
import ae.tawasul.mtproto.components.protocol.rpc.{ClientCommand, ServerCommand}
import cats.effect.{Concurrent, ContextShift, Sync}
import fs2.concurrent.SignallingRef
import fs2.io.tcp.SocketGroup
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import scodec.codecs.CipherFactory

object TcpServer {

  def apply[F[_] : Concurrent : ContextShift](
    socketGroup: SocketGroup,
    port: Int,
  )(implicit cipherFactory: CipherFactory,
  ): F[Unit] = {
    import cats.syntax.functor._
    import cats.syntax.flatMap._

    val loggerF = Slf4jLogger.getLogger[F]

    fs2.Stream
      .eval(Sync[F].delay(new InetSocketAddress(port)))
      .flatMap(inetSocketAddress => socketGroup.server(inetSocketAddress))
      .map(clientResource =>
        for {
          signallingToInterrupt <- fs2.Stream.eval(SignallingRef[F, Boolean](false))
          client <- fs2.Stream.resource(clientResource).interruptWhen(signallingToInterrupt)
          _ <- fs2.Stream.eval(client.remoteAddress.map(_.toString).flatMap(loggerF.info))
          messageSocket <- fs2.Stream.eval(
            MessageSocket[F, Message.Unencrypted[ClientCommand], Message.Unencrypted[ServerCommand]](socket = client)
          )
          _ <- messageSocket.read
            .map(_.messageData)
            .evalMap {
              case _: ClientCommand.ReqDHParams => signallingToInterrupt.set(true).void
              case _: ClientCommand.ReqPQ => messageSocket.write1(Message.Unencrypted(ResPQ.random))
            }
            .drain
        } yield messageSocket
      )
      .parJoin(100)
      .compile
      .drain
  }

}


