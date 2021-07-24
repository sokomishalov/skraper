/*
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
import org.springframework.boot.autoconfigure.web.reactive.function.client.ReactorNettyHttpClientMapper
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import org.springframework.web.reactive.function.client.WebClient
import ru.sokomishalov.commons.core.serialization.OBJECT_MAPPER
import ru.sokomishalov.commons.spring.config.CustomWebFluxConfigurer
import ru.sokomishalov.skraper.Skrapers
import ru.sokomishalov.skraper.bot.telegram.autoconfigure.BotProperties
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.client.spring.SpringReactiveSkraperClient


/**
 * @author sokomishalov
 */
@Configuration
@EnableConfigurationProperties(BotProperties::class)
class SkraperBotConfig : CustomWebFluxConfigurer() {

    @Bean
    @Primary
    fun mapper(): ObjectMapper = OBJECT_MAPPER

    @Bean
    fun insecureSslClientCustomizer(): ReactorNettyHttpClientMapper = ReactorNettyHttpClientMapper { client ->
        client.secure {
            it.sslContext(
                SslContextBuilder.forClient().trustManager(InsecureTrustManagerFactory.INSTANCE).build()
            )
        }
    }

    @Bean
    fun skraperClient(webClientBuider: WebClient.Builder): SkraperClient {
        return SpringReactiveSkraperClient(webClientBuider.build()).also { Skrapers.client = it }
    }
}