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
package ru.sokomishalov.skraper.bot.telegram.web

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.BotApiMethod
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod
import org.telegram.telegrambots.meta.api.objects.Update
import ru.sokomishalov.skraper.bot.telegram.service.SkraperBot
import ru.sokomishalov.skraper.bot.telegram.service.send

/**
 * @author sokomishalov
 */
@RestController
class WebhookController(
    private val bot: SkraperBot,
    private val sender: DefaultAbsSender
) {

    @PostMapping("/webhook")
    suspend fun webhook(@RequestBody update: Update): BotApiMethod<*>? {
        return when (val method = bot.receive(update)) {
            is BotApiMethod<*> -> method
            is PartialBotApiMethod<*> -> withContext<Any?>(IO) { sender.send(method) }.let { null }
            else -> null
        }
    }
}
