package akkahttp.part2_lowlevelserver

import akka.http.scaladsl.{ConnectionContext, HttpsConnectionContext}

import java.security.{KeyStore, SecureRandom}
import javax.net.ssl.{KeyManagerFactory, SSLContext, TrustManagerFactory}

object HttpsContext {

  // step 1: keystore
  private val ks: KeyStore = KeyStore.getInstance("PKCS12")
  private val keyStoreFile = getClass.getClassLoader.getResourceAsStream("keystore.pkcs12")
  private val password = "akka-https".toCharArray
  ks.load(keyStoreFile, password)

  // step 2: initialize a key manager
  private val keyManagerFactory = KeyManagerFactory.getInstance("SunX509")
  keyManagerFactory.init(ks, password)

  // step 3: initialize a trust manager
  private val trustManagerFactory = TrustManagerFactory.getInstance("SunX509")
  trustManagerFactory.init(ks)

  // step 4: initialize an SSL context
  private val sslContext = SSLContext.getInstance("TLS")
  sslContext.init(keyManagerFactory.getKeyManagers, trustManagerFactory.getTrustManagers, new SecureRandom())

  // step 5: return the https connection context
  val httpsConnectionContext: HttpsConnectionContext = ConnectionContext.https(sslContext)

}
