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
package ru.sokomishalov.skraper.bot.telegram.service

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.objects.Update
import ru.sokomishalov.skraper.bot.telegram.autoconfigure.BotProperties

/**
 * @author sokomishalov
 */

@Component
@Primary
@ConditionalOnMissingBean(WebhookSkraperBot::class)
@ConditionalOnProperty("skraper.bot.mode", havingValue = "LONG_POLLING", matchIfMissing = true)
class LongPollingSkraperBot(
    private val bot: SkraperBot,
    private val botProperties: BotProperties
) : TelegramLongPollingBot(botProperties.token) {
    override fun getBotUsername(): String = botProperties.username
    override fun onUpdateReceived(update: Update): Unit = GlobalScope.launch(IO) { with(bot) { receive(update) }.also { send(it) } }.let { }
}