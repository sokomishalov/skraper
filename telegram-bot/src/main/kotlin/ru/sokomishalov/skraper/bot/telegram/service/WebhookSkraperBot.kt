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

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.GetMe
import org.telegram.telegrambots.meta.api.methods.updates.SetWebhook
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.starter.SpringWebhookBot
import ru.sokomishalov.skraper.bot.telegram.autoconfigure.BotProperties
import ru.sokomishalov.skraper.bot.telegram.web.WebhookController

/**
 * @author sokomishalov
 */

@Component
@ConditionalOnProperty("skraper.bot.mode", havingValue = "WEBHOOK", matchIfMissing = false)
class WebhookSkraperBot(
    private val botProperties: BotProperties
) : SpringWebhookBot(SetWebhook.builder().url(botProperties.webhookUrl).build(), botProperties.token) {
    override fun getBotUsername(): String = botProperties.username
    override fun getBotPath(): String = "/webhook"

    /**
     * hook is invoked in [WebhookController]
     */
    override fun onWebhookUpdateReceived(update: Update): BotApiMethod<*> = GetMe()
}