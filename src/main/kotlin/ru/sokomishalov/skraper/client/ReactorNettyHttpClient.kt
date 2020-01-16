package ru.sokomishalov.skraper.client

import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import reactor.netty.http.client.HttpClient
import ru.sokomishalov.skraper.SkraperHttpClient

/**
 * @author sokomishalov
 */
class ReactorNettyHttpClient(
        private val client: HttpClient = DEFAULT_CLIENT
) : SkraperHttpClient {

    override suspend fun fetch(url: String): ByteArray? {
        return client
                .get()
                .uri(url)
                .responseSingle { _, u -> u.asByteArray() }
                .awaitFirstOrNull()
    }

    companion object {
        private val DEFAULT_CLIENT = HttpClient
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
}