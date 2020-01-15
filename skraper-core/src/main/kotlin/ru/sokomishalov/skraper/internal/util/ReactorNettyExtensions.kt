package ru.sokomishalov.skraper.internal.util

import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import reactor.netty.http.client.HttpClient

/**
 * @author sokomishalov
 */

val REACTIVE_NETTY_HTTP_CLIENT: HttpClient by lazy {
    HttpClient
            .create()
            .followRedirect(true)
            .secure {
                it.sslContext(SslContextBuilder
                        .forClient()
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .build()
                )
            }
}

suspend fun fetch(url: String): ByteArray? {
    return REACTIVE_NETTY_HTTP_CLIENT
            .get()
            .uri(url)
            .responseSingle { _, u -> u.asByteArray() }
            .awaitFirstOrNull()
}