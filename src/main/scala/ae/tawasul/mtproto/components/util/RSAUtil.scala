package ae.tawasul.mtproto.components.util

import java.security.spec.{PKCS8EncodedKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PrivateKey, PublicKey}
import java.util.Base64

import scala.util.Try

object RSAUtil {

  def getPublicKey(base64PublicKey: String): Try[PublicKey] = Try {
    val keySpec = new X509EncodedKeySpec(Base64.getDecoder.decode(base64PublicKey.getBytes))
    val keyFactory = KeyFactory.getInstance("RSA")
    keyFactory.generatePublic(keySpec)
  }

  def getPrivateKey(base64PrivateKey: String): Try[PrivateKey] = Try {
    val keySpec = new PKCS8EncodedKeySpec(Base64.getDecoder.decode(base64PrivateKey.getBytes))
    val keyFactory = KeyFactory.getInstance("RSA")
    keyFactory.generatePrivate(keySpec)
  }

}
