package ae.tawasul.mtproto.components.protocol

import java.time.Instant

import scodec.bits._
import scodec.codecs._
import scodec.{Attempt, Codec, DecodeResult, Err, SizeBound}


object mtProtoTransport {

  sealed trait Message
  object Message {

    case class Unencrypted[PAYLOAD](
      messageId: Instant,
      messageData: PAYLOAD,
    ) extends Message

    object Unencrypted {

      def apply[PAYLOAD](payload: PAYLOAD): Unencrypted[PAYLOAD] = Unencrypted(Instant.now(), payload)

      implicit def unencryptedMessageCodec[PAYLOAD: Codec]: Codec[Unencrypted[PAYLOAD]] = {
        val authKeyIdCodec = constant(ByteVector.fromLong(0L, ordering = ByteOrdering.LittleEndian))

        val messageIdCodec = int64L.xmap[Instant](Instant.ofEpochMilli, _.toEpochMilli)
        val body = (messageIdCodec :: variableSizeBytes(int32L, Codec[PAYLOAD])).as[Unencrypted[PAYLOAD]]

        authKeyIdCodec
          .flatZip(_ => body)
          .widenc[Unencrypted[PAYLOAD]]({case (_, data) => data})(data => Attempt.Successful(((), data)))
      }
    }

  }

  sealed trait Protocol
  object Protocol {

    case class Abridged[PAYLOAD](
      payload: PAYLOAD
    ) extends Protocol

    object Abridged {
      private val protocolByteValue: ByteVector = hex"0x7f"
      val protocolIntValue: ByteVector = protocolByteValue.padTo(4)

      implicit def abridgedProtocolCodec[PAYLOAD: Codec]: Codec[Abridged[PAYLOAD]] = new Codec[Abridged[PAYLOAD]] {
        override def sizeBound: SizeBound = SizeBound.atLeast(1L)

        val lengthDivided = 4

        override def encode(value: Abridged[PAYLOAD]): Attempt[BitVector] = {
          Codec[PAYLOAD].encode(value.payload).flatMap { bitVector =>
            val divided = bitVector.toByteVector.size / lengthDivided
            if (divided < 127L) {
              byte.encode(divided.toByte).map(_ ++ bitVector)
            } else {
              int24L.encode(divided.toInt).map(length => protocolByteValue.toBitVector ++ length ++ bitVector)
            }
          }
        }

        override def decode(bits: BitVector): Attempt[DecodeResult[Abridged[PAYLOAD]]] = {
          val protocolByteMet = bits.startsWith(protocolByteValue.toBitVector)
          val lengthCodec = if (protocolByteMet) {
            int24L.xmap[Int](_ * lengthDivided, _ / lengthDivided)
          } else {
            int8L.xmap[Int](_ * lengthDivided, _ / lengthDivided)
          }
          val toParse = if (protocolByteMet) bits.drop(8) else bits
          variableSizeBytes(lengthCodec, Codec[PAYLOAD]).decode(toParse).map(_.map(Abridged.apply))
        }
      }
    }

    case class Intermediate[PAYLOAD](
      payload: PAYLOAD
    ) extends Protocol

    object Intermediate {
      val protocolIntValue = hex"0xeeeeeeee"
      implicit def intermediateProtocolCodec[PAYLOAD: Codec]: Codec[Intermediate[PAYLOAD]] = {
        variableSizeBytes(int32L, Codec[PAYLOAD]).as[Intermediate[PAYLOAD]]
      }
    }

    case class Full[PAYLOAD](
      tcpSequenceNumber: Int,
      payload: PAYLOAD,
    ) extends Protocol

    object Full {
      val protocolIntValue = hex"0x00000000"

      implicit def fullProtocolCodec[PAYLOAD: Codec]: Codec[Full[PAYLOAD]] = new Codec[Full[PAYLOAD]] {
        override def sizeBound: SizeBound = SizeBound.atLeast(12L)

        private val sizeFieldBytesLength = 4
        private val crc32FieldBytesLength = 4
        private val additionalBytes = sizeFieldBytesLength + crc32FieldBytesLength
        private val sizeCodec = int32L.xmap[Int](_ - additionalBytes, _ + additionalBytes)
        private val fullProtocolCodec = (int32L :: Codec[PAYLOAD]).as[Full[PAYLOAD]]

        override def encode(value: Full[PAYLOAD]): Attempt[BitVector] = {
          variableSizeBytes(sizeCodec, fullProtocolCodec)
            .encode(value)
            .map{ bits => bits ++ scodec.bits.crc.crc32(bits).reverseByteOrder }
        }

        override def decode(bits: BitVector): Attempt[DecodeResult[Full[PAYLOAD]]] = {
          for {
            decoded <- variableSizeBytes(sizeCodec, scodec.codecs.bits).decode(bits)
            payloadDecoded <- fullProtocolCodec.decode(decoded.value)
            crc32Decoded <- bytes(4).decode(decoded.remainder)
            result <- {
              val crc32 = crc32Decoded.value.toBitVector
              val payload = payloadDecoded.value
              val sizeBits = bits.take(32)
              val computedCrc32 = scodec.bits.crc.crc32(sizeBits ++ decoded.value).reverseByteOrder
              if (computedCrc32 == crc32) {
                Attempt.successful(DecodeResult(payload, crc32Decoded.remainder))
              } else {
                Attempt.failure(Err(s"message is corrupted: expected crc32 $crc32 but met $computedCrc32"))
              }
            }
          } yield result
        }
      }

    }

  }

}
