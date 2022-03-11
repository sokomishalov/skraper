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
@file:Suppress(
    "FoldInitializerAndIfToElvis",
    "BlockingMethodInNonBlockingContext"
)

package ru.sokomishalov.skraper.bot.telegram.service

import kotlinx.coroutines.flow.firstOrNull
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.*
import org.telegram.telegrambots.meta.api.objects.InputFile
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.api.objects.media.InputMediaPhoto
import org.telegram.telegrambots.meta.api.objects.media.InputMediaVideo
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.Skrapers
import ru.sokomishalov.skraper.internal.net.path
import ru.sokomishalov.skraper.model.*
import java.io.File
import java.nio.file.Files.createTempDirectory
import kotlin.text.RegexOption.*

/**
 * @author sokomishalov
 */
@Service
class SkraperBot {

    suspend fun receive(update: Update): PartialBotApiMethod<*>? {
        // 0. say hello
        val message = update.message ?: return null
        if (message.text.orEmpty() == "/start") return sendText(message, "Hello!")

        // 1. extract url
        val url = extractUrlFromMessage(message.text.orEmpty())
        if (url.isNullOrBlank()) return sendText(message, "URL not found in the message")

        // 2. find suitable skraper
        val supportedSkraper: Skraper? = Skrapers.findSuitable(url)
        if (supportedSkraper == null) return sendText(message, "Unsupported URL")

        // 3. try to either scrape posts and download attachments or just download attachment
        val latestPost = runCatching { supportedSkraper.getPosts(path = url.path).firstOrNull() }.getOrNull()
        val tmpDir = createTempDirectory("skraper-bot").toFile()

        return runCatching {
            when {
                latestPost != null -> {
                    latestPost
                        .media
                        .associateWith { media ->
                            Skrapers.download(
                                media = media,
                                destDir = tmpDir
                            )
                        }
                        .let { items ->
                            when {
                                items.isNotEmpty() -> sendMedia(message, items)
                                else -> saySorry(message)
                            }
                        }
                }

                else -> {
                    listOf(Video(url), Audio(url), Image(url))
                        .firstNotNullOfOrNull { media -> media to Skrapers.resolve(media = media) }
                        ?.let { (_, resolved) ->
                            val file = Skrapers.download(media = resolved, destDir = tmpDir)
                            sendMedia(message, mapOf(resolved to file))
                        }
                        ?: saySorry(message)

                }
            }
        }.getOrElse {
            logger.error(it.message, it)
            saySorry(message)
        }
    }

    private fun extractUrlFromMessage(text: String): String? {
        return URL_REGEX
            .find(text)
            ?.groupValues
            ?.firstOrNull()
            ?.trim()
    }

    private fun sendText(message: Message, msg: String): SendMessage {
        return SendMessage().apply {
            chatId = message.chatId.toString()
            text = msg
            replyToMessageId = message.messageId
        }
    }

    private fun sendMedia(
        message: Message,
        attachments: Map<Media, File>
    ): PartialBotApiMethod<*>? {
        return when (attachments.size) {
            0 -> null
            1 -> {
                val (media, file) = attachments.entries.first()
                when (media) {
                    is Image -> SendPhoto().apply {
                        replyToMessageId = message.messageId
                        chatId = message.chatId.toString()
                        photo = InputFile(file, file.nameWithoutExtension)
                    }
                    is Video -> SendVideo().apply {
                        replyToMessageId = message.messageId
                        chatId = message.chatId.toString()
                        video = InputFile(file, file.nameWithoutExtension)
                    }
                    is Audio -> SendAudio().apply {
                        replyToMessageId = message.messageId
                        chatId = message.chatId.toString()
                        audio = InputFile(file, file.nameWithoutExtension)
                    }
                    is UnknownMedia -> SendMessage().apply {
                        replyToMessageId = message.messageId
                        chatId = message.chatId.toString()
                        text = media.url
                    }
                }
            }
            else -> {
                SendMediaGroup().apply {
                    replyToMessageId = message.messageId
                    chatId = message.chatId.toString()
                    medias = attachments.mapNotNull { (media, file) ->
                        when (media) {
                            is Image -> InputMediaPhoto().apply {
                                setMedia(file, file.nameWithoutExtension)
                            }
                            is Video -> InputMediaVideo().apply {
                                setMedia(file, file.nameWithoutExtension)
                            }
                            is Audio -> InputMediaPhoto().apply {
                                setMedia(file, file.nameWithoutExtension)
                            }
                            is UnknownMedia -> null
                        }
                    }
                }
            }
        }
    }

    private fun saySorry(message: Message): SendMessage {
        return sendText(message, "Unable to download media for this link, sorry :(")
    }

    companion object {
        private val logger = LoggerFactory.getLogger(SkraperBot::class.java)
        private val URL_REGEX: Regex = "(?:^|[\\W])((ht)tp(s?)://|www\\.)(([\\w\\-]+\\.)+?([\\w\\-.~]+/?)*[\\p{Alnum}.,%_=?&#\\-+()\\[\\]*$~@!:/{};']*)".toRegex(setOf(IGNORE_CASE, MULTILINE, DOT_MATCHES_ALL))
    }
}