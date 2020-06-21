package ae.tawasul.mtproto.components.model

import supertagged.TaggedType

import scala.util.Try

object domain {
  import cats.syntax.either._

  object Int128 extends TaggedType[BigInt] {
    def from(bigInt: BigInt): Either[String, Int128] = toBigint(bigInt)
    def unsafeFrom(bigInt: BigInt): Int128 = from(bigInt).fold(e => throw new IllegalArgumentException(e), identity)

    def from(bytes: Array[Byte]): Either[String, Int128] = toBigint(BigInt(bytes))
    def unsafeFrom(bytes: Array[Byte]): Int128 = from(bytes).fold(e => throw new IllegalArgumentException(e), identity)

    def from(string: String): Either[String, Int128] = toBigint(BigInt(string))
    def unsafeFrom(string: String): Int128 = from(string).fold(e => throw new IllegalArgumentException(e), identity)

    val min: Int128 = Int128 @@ BigInt("-170141183460469231731687303715884105728")
    val max: Int128 = Int128 @@ BigInt("170141183460469231731687303715884105727")

    private def toBigint[T](value: => BigInt) = {
      Try(value).toEither
        .map(Int128 @@ _)
        .leftMap(_.getMessage)
        .flatMap { value =>
          if (value >= min || value <= max) Right(value)
          else Left(s"$value is out of 128-bit signed integer")
        }
    }

  }
  type Int128 = Int128.Type

}
