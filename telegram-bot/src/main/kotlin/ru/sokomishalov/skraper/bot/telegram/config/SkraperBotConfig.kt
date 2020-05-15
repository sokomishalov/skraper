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

import org.springframework.boot.autoconfigure.AutoConfigureOrder
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered.HIGHEST_PRECEDENCE
import org.telegram.telegrambots.meta.generics.LongPollingBot
import org.telegram.telegrambots.meta.generics.WebhookBot
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.bot.telegram.autoconfigure.BotProperties
import ru.sokomishalov.skraper.bot.telegram.service.LongPollingSkraperBot
import ru.sokomishalov.skraper.bot.telegram.service.SkraperBot
import ru.sokomishalov.skraper.bot.telegram.service.WebhookSkraperBot
import ru.sokomishalov.skraper.client.ktor.KtorSkraperClient
import ru.sokomishalov.skraper.knownList

/**
 * @author sokomishalov
 */
@Configuration
@EnableConfigurationProperties(BotProperties::class)
@AutoConfigureOrder(HIGHEST_PRECEDENCE)
class SkraperBotConfig {

    @Bean
    fun ktorClient(): SkraperClient {
        return KtorSkraperClient()
    }

    @Bean
    fun knownSkrapers(ktor: SkraperClient): List<Skraper> {
        return Skraper.knownList(ktor)
    }

    @Bean
    fun bot(knownSkrapers: List<Skraper>, botProperties: BotProperties): SkraperBot {
        return SkraperBot(knownSkrapers = knownSkrapers, botProperties = botProperties)
    }

    @Bean
    @ConditionalOnProperty("skraper.bot.mode", havingValue = "WEBHOOK", matchIfMissing = false)
    fun webhookBot(bot: SkraperBot, botProperties: BotProperties): WebhookBot {
        return WebhookSkraperBot(bot = bot, botProperties = botProperties).apply {
            setWebhook(botProperties.webhookUrl, null)
        }
    }

    @Bean
    @ConditionalOnMissingBean(WebhookSkraperBot::class)
    @ConditionalOnProperty("skraper.bot.mode", havingValue = "LONG_POLLING", matchIfMissing = true)
    fun longPollingBot(bot: SkraperBot, botProperties: BotProperties): LongPollingBot {
        return LongPollingSkraperBot(bot = bot, botProperties = botProperties)
    }
}