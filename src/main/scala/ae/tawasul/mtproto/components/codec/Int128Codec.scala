package ae.tawasul.mtproto.components.codec

import ae.tawasul.mtproto.components.model.domain.Int128
import scodec.{Attempt, Codec, DecodeResult, SizeBound}
import scodec.bits.{BitVector, ByteOrdering}

object Int128Codec extends Codec[Int128] {

  private val bits = 128
  private val codec = new SignedBigIntCodec(bits, ByteOrdering.LittleEndian)

  override val sizeBound: SizeBound = codec.sizeBound

  override def encode(u: Int128): Attempt[BitVector] = {
    codec.encode(u)
  }

  override def decode(bits: BitVector): Attempt[DecodeResult[Int128]] = {
    codec.decode(bits).map(_.map(Int128.unsafeFrom))
  }

  override def toString = codec.toString
}

