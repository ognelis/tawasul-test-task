package ae.tawasul.mtproto

import ae.tawasul.mtproto.components.server.TcpServer
import ae.tawasul.mtproto.components.util.RSAUtil
import cats.effect.{Blocker, ExitCode, IO, IOApp, Sync}
import fs2.io.tcp.SocketGroup
import io.chrisdavenport.log4cats.slf4j.Slf4jLogger
import javax.crypto.Cipher
import scodec.codecs._

object App extends IOApp {

  type F[T] = IO[T]

  private val rsaPublicKey = {
    "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCgFGVfrY4jQSoZQWWygZ83roKXWD4YeT2x2p41dGkPixe73rT2IW04glagN2vgoZoHuOPqa5and6kAmK2ujmCHu6D1auJhE2tXP+yLkpSiYMQucDKmCsWMnW9XlC5K7OSL77TXXcfvTvyZcjObEz6LIBRzs6+FqpFbUO9SJEfh6wIDAQAB"
  }

  private val rsaPrivateKey = {
    "MIICdQIBADANBgkqhkiG9w0BAQEFAASCAl8wggJbAgEAAoGBAKAUZV+tjiNBKhlBZbKBnzeugpdYPhh5PbHanjV0aQ+LF7vetPYhbTiCVqA3a+Chmge44+prlqd3qQCYra6OYIe7oPVq4mETa1c/7IuSlKJgxC5wMqYKxYydb1eULkrs5IvvtNddx+9O/JlyM5sTPosgFHOzr4WqkVtQ71IkR+HrAgMBAAECgYAkQLo8kteP0GAyXAcmCAkA2Tql/8wASuTX9ITD4lsws/VqDKO64hMUKyBnJGX/91kkypCDNF5oCsdxZSJgV8owViYWZPnbvEcNqLtqgs7nj1UHuX9S5yYIPGN/mHL6OJJ7sosOd6rqdpg6JRRkAKUV+tmN/7Gh0+GFXM+ug6mgwQJBAO9/+CWpCAVoGxCA+YsTMb82fTOmGYMkZOAfQsvIV2v6DC8eJrSa+c0yCOTa3tirlCkhBfB08f8U2iEPS+Gu3bECQQCrG7O0gYmFL2RX1O+37ovyyHTbst4s4xbLW4jLzbSoimL235lCdIC+fllEEP96wPAiqo6dzmdH8KsGmVozsVRbAkB0ME8AZjp/9Pt8TDXD5LHzo8mlruUdnCBcIo5TMoRG2+3hRe1dHPonNCjgbdZCoyqjsWOiPfnQ2Brigvs7J4xhAkBGRiZUKC92x7QKbqXVgN9xYuq7oIanIM0nz/wq190uq0dh5Qtow7hshC/dSK3kmIEHe8z++tpoLWvQVgM538apAkBoSNfaTkDZhFavuiVl6L8cWCoDcJBItip8wKQhXwHp0O3HLg10OEd14M58ooNfpgt+8D8/8/2OOFaR0HzA+2Dm"
  }

  private val port = 80

  override def run(args: List[String]): F[ExitCode] = {

    for {
      loggerF <- Slf4jLogger.create[F]
      _ <- loggerF.info("App started")
      publicKey <- Sync[F].fromTry(RSAUtil.getPublicKey(rsaPublicKey))
      privateKey <- Sync[F].fromTry(RSAUtil.getPrivateKey(rsaPrivateKey))
      cipherFactory <- Sync[F].delay {
        CipherFactory(
          transformation = "RSA/ECB/NoPadding",
          initForEncryption = _.init(Cipher.ENCRYPT_MODE, publicKey),
          initForDecryption = _.init(Cipher.DECRYPT_MODE, privateKey),
        )
      }
      exitCode <- Blocker[F].use { blocker =>
        SocketGroup[F](blocker).use { socketGroup =>
          implicit val cf = cipherFactory
          TcpServer[F](socketGroup, port).as(ExitCode.Success)
        }
      }
    } yield exitCode
  }
}
