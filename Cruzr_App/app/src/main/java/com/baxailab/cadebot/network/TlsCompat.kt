package com.baxailab.cadebot.network

import android.content.Context
import com.baxailab.cadebot.R
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Android 5.1 does not always have the current Let's Encrypt root in its
 * system trust store. Keep the platform trust store and add the public ISRG
 * roots needed by the Cadebot HTTPS gateway.
 */
object TlsCompat {
    data class Config(
        val sslContext: SSLContext,
        val trustManager: X509TrustManager
    )

    fun create(context: Context): Config {
        val systemTrustManager = defaultTrustManager()
        val bundledKeyStore = KeyStore.getInstance(KeyStore.getDefaultType()).apply {
            load(null, null)
        }
        val certificateFactory = CertificateFactory.getInstance("X.509")

        listOf(R.raw.isrg_root_x1, R.raw.isrg_root_x2).forEachIndexed { index, resourceId ->
            context.resources.openRawResource(resourceId).use { input ->
                bundledKeyStore.setCertificateEntry(
                    "cadebot-isrg-root-$index",
                    certificateFactory.generateCertificate(input)
                )
            }
        }

        val bundledFactory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        )
        bundledFactory.init(bundledKeyStore)
        val bundledTrustManager = bundledFactory.trustManagers
            .filterIsInstance<X509TrustManager>()
            .single()

        val compositeTrustManager = object : X509TrustManager {
            override fun checkClientTrusted(
                chain: Array<java.security.cert.X509Certificate>,
                authType: String
            ) = systemTrustManager.checkClientTrusted(chain, authType)

            override fun checkServerTrusted(
                chain: Array<java.security.cert.X509Certificate>,
                authType: String
            ) {
                try {
                    systemTrustManager.checkServerTrusted(chain, authType)
                } catch (systemFailure: CertificateException) {
                    try {
                        bundledTrustManager.checkServerTrusted(chain, authType)
                    } catch (_: CertificateException) {
                        throw systemFailure
                    }
                }
            }

            override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> =
                (systemTrustManager.acceptedIssuers.asList() +
                    bundledTrustManager.acceptedIssuers.asList())
                    .distinctBy { it.subjectX500Principal.name }
                    .toTypedArray()
        }

        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(compositeTrustManager), null)
        }
        return Config(sslContext, compositeTrustManager)
    }

    private fun defaultTrustManager(): X509TrustManager {
        val factory = TrustManagerFactory.getInstance(
            TrustManagerFactory.getDefaultAlgorithm()
        )
        factory.init(null as KeyStore?)
        return factory.trustManagers.filterIsInstance<X509TrustManager>().single()
    }
}
