package ae.tawasul.mtproto.components.codec

import scodec.bits._
import scodec.{Attempt, Codec, DecodeResult, SizeBound, codecs}

object LongVectorCodec extends Codec[Vector[Long]] {

  private val vectorCodec: Codec[Vector[Long]] = scodec
    .codecs.constant(hex"1cb5c415".padTo(4))
    .flatZip(_ => codecs.vectorOfN(codecs.int32L, codecs.int64L))
    .widen({case (_, vector) => vector}, vector => Attempt.Successful(((), vector)))

  override def decode(bits: BitVector): Attempt[DecodeResult[Vector[Long]]] = vectorCodec.decode(bits)

  override def encode(value: Vector[Long]): Attempt[BitVector] = vectorCodec.encode(value)

  override def sizeBound: SizeBound = vectorCodec.sizeBound
}
