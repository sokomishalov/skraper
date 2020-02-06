/**
 * Copyright 2019-2020 the original author or authors.
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
package ru.sokomishalov.skraper.provider.youtube

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.internal.jsoup.getSingleElementByClassOrNull
import ru.sokomishalov.skraper.internal.url.uriCleanUp
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.VIDEO
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.Post
import java.lang.System.currentTimeMillis
import java.time.Duration
import java.time.Period
import java.time.chrono.ChronoPeriod
import java.time.temporal.ChronoUnit.DAYS
import java.time.temporal.TemporalAmount

class YoutubeSkraper(
        override val client: SkraperClient = DefaultBlockingSkraperClient
) : Skraper {

    override val baseUrl: String = "https://www.youtube.com"

    override suspend fun getLatestPosts(uri: String, limit: Int): List<Post> {
        val document = getUserPage(uri)

        val videos = document
                ?.getElementsByClass("yt-lockup-video")
                ?.take(limit)
                .orEmpty()

        return videos.map {
            val linkElement = it.getElementsByClass("yt-uix-tile-link").firstOrNull()

            Post(
                    id = linkElement.parseId(),
                    caption = linkElement.parseCaption(),
                    publishTimestamp = it.parsePublishDate(),
                    attachments = listOf(Attachment(
                            url = "${baseUrl}${linkElement?.attr("href")}",
                            type = VIDEO,
                            aspectRatio = YOUTUBE_DEFAULT_VIDEO_ASPECT_RATIO
                    ))
            )
        }
    }

    override suspend fun getPageLogoUrl(uri: String, imageSize: ImageSize): String? {
        val document = getUserPage(uri)

        return document
                ?.getElementsByAttributeValue("rel", "image_src")
                ?.firstOrNull()
                ?.attr("href")
    }

    private suspend fun getUserPage(uri: String): Document? {
        val finalUri = when {
            uri.endsWith("/videos") -> "${uri.uriCleanUp()}?gl=EN&hl=en"
            else -> "${uri.uriCleanUp()}/videos?gl=EN&hl=en"
        }

        return client.fetchDocument("${baseUrl}/${finalUri}")
    }

    private fun Element?.parseId(): String {
        return this
                ?.attr("href")
                ?.substringAfter("/watch?v=")
                .orEmpty()
    }

    private fun Element?.parseCaption(): String {
        return this
                ?.attr("title")
                .orEmpty()
    }

    private fun Element?.parsePublishDate(): Long? {
        return this
                ?.getSingleElementByClassOrNull("yt-lockup-meta-info")
                ?.getElementsByTag("li")
                ?.getOrNull(1)
                ?.wholeText()
                ?.let {
                    val now = currentTimeMillis()

                    val amount = it.split(" ")
                            .firstOrNull()
                            ?.toIntOrNull()
                            ?: 1

                    val temporalAmount: TemporalAmount = when {
                        it.contains("moment", ignoreCase = true) or it.contains("moments", ignoreCase = true) -> Duration.ofMillis(amount.toLong())
                        it.contains("second", ignoreCase = true) or it.contains("seconds", ignoreCase = true) -> Duration.ofSeconds(amount.toLong())
                        it.contains("minute", ignoreCase = true) or it.contains("minute", ignoreCase = true) -> Duration.ofMinutes(amount.toLong())
                        it.contains("hour", ignoreCase = true) or it.contains("hour", ignoreCase = true) -> Duration.ofHours(amount.toLong())
                        it.contains("day", ignoreCase = true) or it.contains("day", ignoreCase = true) -> Duration.ofDays(amount.toLong())
                        it.contains("week", ignoreCase = true) or it.contains("week", ignoreCase = true) -> Period.ofWeeks(amount)
                        it.contains("month", ignoreCase = true) or it.contains("month", ignoreCase = true) -> Period.ofMonths(amount)
                        it.contains("year", ignoreCase = true) or it.contains("year", ignoreCase = true) -> Period.ofYears(amount)
                        else -> Duration.ZERO
                    }
                    val millisAgo = when (temporalAmount) {
                        is Duration -> temporalAmount.toMillis()
                        is Period -> Duration.ofDays(temporalAmount.get(DAYS)).toMillis()
                        is ChronoPeriod -> Duration.ofDays(temporalAmount.get(DAYS)).toMillis()
                        else -> 0
                    }
                    now - millisAgo
                }
    }

    companion object {
        private const val YOUTUBE_DEFAULT_VIDEO_ASPECT_RATIO = 210 / 117.5
    }
}