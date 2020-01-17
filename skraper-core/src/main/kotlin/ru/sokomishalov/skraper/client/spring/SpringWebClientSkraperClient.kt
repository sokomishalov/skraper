@file:Suppress("unused")

package ru.sokomishalov.skraper.client.spring

import kotlinx.coroutines.reactive.awaitFirstOrNull
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.bodyToMono
import ru.sokomishalov.skraper.SkraperClient

/**
 * @author sokomishalov
 */
class SpringWebClientSkraperClient(
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