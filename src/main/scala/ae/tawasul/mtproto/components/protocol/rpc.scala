package ae.tawasul.mtproto.components.protocol

import ae.tawasul.mtproto.components.model.domain.Int128
import scodec.bits._
import scodec.codecs.{CipherFactory, _}
import scodec.{Codec, codecs}

object rpc {

  import ae.tawasul.mtproto.components.codec._

  sealed trait ClientCommand

  object ClientCommand {

    case class ReqPQ(nonce: Int128) extends ClientCommand

    object ReqPQ {
      implicit val reqPQCodec: Codec[ReqPQ] = Int128Codec.as[ReqPQ]
    }

    case class ReqDHParams(
      nonce: Int128,
      serverNonce: Int128,
      p: String, //First prime cofactor
      q: String, //Second prime cofactor
      value: String,
      publicKeyFingerprint: Long,
      encryptedData: String,
    ) extends ClientCommand

    object ReqDHParams {
      implicit def reqDHParamsCodec(implicit cipherFactory: CipherFactory): Codec[ReqDHParams] = {
        val codec =  (Int128Codec :: Int128Codec :: ascii32 :: ascii32 :: ascii32 :: int64L :: ascii32).as[ReqDHParams]
        codecs.encrypted(codec)
      }
    }

    implicit def clientCommandCodec(implicit cipherFactory: CipherFactory): Codec[ClientCommand] = discriminated[ClientCommand]
      .by(bytes(4))
      .typecase(hex"60469778", Codec[ReqPQ])
      .typecase(hex"d712e4be", Codec[ReqDHParams])
  }

  sealed trait ServerCommand

  object ServerCommand {

    case class ResPQ(
      nonce: Int128,
      serverNonce: Int128,
      pq: String,
      serverPublicKeyFingerprints: Vector[Long]
    ) extends ServerCommand

    object ResPQ {
      implicit val resPQCodec: Codec[ResPQ] = {
        (Int128Codec :: Int128Codec :: ascii32 :: LongVectorCodec).as[ResPQ]
      }

      def random: ResPQ = {
        val rand = new scala.util.Random()
        val nonce = Int128.unsafeFrom(rand.nextLong())
        val serverNonce = Int128.unsafeFrom(rand.nextLong())
        val pq = rand.nextString(9)
        val serverPublicKeyFingerprints = Vector.fill(rand.nextInt(10))(rand.nextLong())
        ResPQ(
          nonce = nonce,
          serverNonce = serverNonce,
          pq = pq,
          serverPublicKeyFingerprints = serverPublicKeyFingerprints,
        )
      }
    }

    implicit val serverCommandCodec: Codec[ServerCommand] = discriminated[ServerCommand]
      .by(bytes(4))
      .typecase(hex"05162463", Codec[ResPQ])
  }

}
