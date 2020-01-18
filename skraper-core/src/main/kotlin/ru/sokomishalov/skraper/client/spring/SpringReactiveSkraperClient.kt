@file:Suppress("unused")

package ru.sokomishalov.skraper.client.spring

import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import reactor.netty.http.client.HttpClient
import ru.sokomishalov.skraper.SkraperClient

/**
 * @author sokomishalov
 */
class SpringReactiveSkraperClient(
        private val webClient: WebClient = DEFAULT_CLIENT
) : SkraperClient {

    override suspend fun fetch(url: String): ByteArray? {
        return webClient
                .get()
                .uri(url)
                .exchange()
                .awaitFirstOrNull()
                ?.bodyToMono<ByteArray>()
                ?.awaitFirstOrNull()
    }

    companion object {
        private val DEFAULT_CLIENT: WebClient = WebClient
                .builder()
                .clientConnector(
                        ReactorClientHttpConnector(
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
                        )
                )
                .exchangeStrategies(ExchangeStrategies
                        .builder()
                        .codecs { ccc ->
                            ccc.defaultCodecs().apply {
                                maxInMemorySize(16 * 1024 * 1024)
                            }
                        }
                        .build()
                )
                .build()
    }
}