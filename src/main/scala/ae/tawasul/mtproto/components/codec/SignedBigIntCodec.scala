package ae.tawasul.mtproto.components.codec

import java.nio.{ByteBuffer, ByteOrder}

import scodec.{Attempt, Codec, DecodeResult, Err, SizeBound}
import scodec.bits.{BitVector, ByteOrdering}

final class SignedBigIntCodec(
  bits: Int,
  ordering: ByteOrdering,
) extends Codec[BigInt] {

  val MaxValue: BigInt = (BigInt(1) << (bits - 1)) - 1
  val MinValue: BigInt = -(BigInt(1) << (bits - 1))

  private val bitsL = bits.toLong

  override def sizeBound: SizeBound = SizeBound.exact(bitsL)

  private def description = s"$bits-bit signed integer"

  override def encode(i: BigInt): Attempt[BitVector] = {

    def fromInt128(i: BigInt): BitVector = {
      val toAllocate = (bits / 8) + (if (bits % 8 > 0) 1 else 0)
      val buffer = ByteBuffer.allocate(toAllocate).order(ByteOrder.BIG_ENDIAN).put(i.toByteArray)
      buffer.flip()
      val relevantBits = BitVector.view(buffer)
      if (ordering == ByteOrdering.BigEndian) relevantBits else relevantBits.reverseByteOrder
    }

    if (i > MaxValue) {
      Attempt.failure(Err(s"$i is greater than maximum value $MaxValue for $description"))
    } else if (i < MinValue) {
      Attempt.failure(Err(s"$i is less than minimum value $MinValue for $description"))
    } else {
      Attempt.successful(fromInt128(i))
    }
  }

  override def decode(buffer: BitVector): Attempt[DecodeResult[BigInt]] =
    if (buffer.sizeGreaterThanOrEqual(bitsL))
      Attempt.successful(
        ordering match {
          case ByteOrdering.BigEndian =>
            DecodeResult(BigInt(buffer.take(bitsL).toByteArray), buffer.drop(bitsL))
          case ByteOrdering.LittleEndian =>
            DecodeResult(BigInt(buffer.take(bitsL).reverseByteOrder.toByteArray), buffer.drop(bitsL))
        }
      )
    else
      Attempt.failure(Err.insufficientBits(bitsL, buffer.size))

  override def toString: String = description
}
