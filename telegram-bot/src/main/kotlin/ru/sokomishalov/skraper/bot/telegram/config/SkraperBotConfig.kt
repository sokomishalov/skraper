/**
 * Copyright (c) 2019-present Mikhael Sokolov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.sokomishalov.skraper.bot.telegram.config

import com.fasterxml.jackson.databind.ObjectMapper
import io.netty.handler.ssl.SslContextBuilder
import io.netty.handler.ssl.util.InsecureTrustManagerFactory
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.Jackson2JsonDecoder
import org.springframework.http.codec.json.Jackson2JsonEncoder
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient
import ru.sokomishalov.commons.core.serialization.OBJECT_MAPPER
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.bot.telegram.autoconfigure.BotProperties
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.client.spring.SpringReactiveSkraperClient
import ru.sokomishalov.skraper.knownList


/**
 * @author sokomishalov
 */
@Configuration
@EnableConfigurationProperties(BotProperties::class)
class SkraperBotConfig {

    @Bean
    @Primary
    fun mapper(): ObjectMapper {
        return OBJECT_MAPPER
    }

    @Bean
    @Primary
    fun webClient(mapper: ObjectMapper): WebClient {
        return WebClient
            .builder()
            .clientConnector(ReactorClientHttpConnector(
                HttpClient
                    .create()
                    .secure {
                        it.sslContext(
                            SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
                        )
                    }
            ))
            .exchangeStrategies(ExchangeStrategies
                .builder()
                .codecs {
                    it.defaultCodecs().apply {
                        maxInMemorySize(-1)
                        jackson2JsonDecoder(Jackson2JsonDecoder(mapper))
                        jackson2JsonEncoder(Jackson2JsonEncoder(mapper))
                    }
                }
                .build()
            )
            .build()
    }

    @Bean
    fun skraperClient(webClient: WebClient): SkraperClient {
        return SpringReactiveSkraperClient(webClient)
    }

    @Bean
    fun knownSkrapers(client: SkraperClient): List<Skraper> {
        return Skraper.knownList(client)
    }
}