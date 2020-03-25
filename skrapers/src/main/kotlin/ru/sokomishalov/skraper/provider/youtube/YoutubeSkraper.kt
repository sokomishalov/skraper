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
package ru.sokomishalov.skraper.provider.youtube

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByAttributeValue
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByClass
import ru.sokomishalov.skraper.model.*
import ru.sokomishalov.skraper.model.MediaSize.*
import java.time.Duration
import java.time.Duration.ZERO
import java.time.Period
import java.time.chrono.ChronoPeriod
import java.time.temporal.ChronoUnit.DAYS
import java.time.temporal.TemporalAmount

class YoutubeSkraper @JvmOverloads constructor(
        override val client: SkraperClient = DefaultBlockingSkraperClient,
        override val baseUrl: URLString = "https://www.youtube.com"
) : Skraper {

    override suspend fun getPosts(path: String, limit: Int): List<Post> {
        val page = getUserPage(path = path)

        val videos = page
                ?.getElementsByClass("yt-lockup-video")
                ?.take(limit)
                .orEmpty()

        return videos.map {
            val linkElement = it.getFirstElementByClass("yt-uix-tile-link")

            Post(
                    id = linkElement.extractPostId(),
                    text = linkElement.extractPostCaption(),
                    viewsCount = it.extractPostViewsCount(),
                    publishedAt = it.extractPostPublishDate(),
                    media = it.extractPostVideos()
            )
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val page = getUserPage(path = path)

        return page?.run {
            PageInfo(
                    nick = extractPageNick(),
                    name = extractPageName(),
                    description = extractPageDescription(),
                    followersCount = extractFollowersCount(),
                    avatarsMap = extractPageAvatarsMap(),
                    coversMap = extractPageCoversMap()
            )
        }
    }

    private suspend fun getUserPage(path: String): Document? {
        return client.fetchDocument(url = baseUrl.buildFullURL(
                path = path,
                queryParams = mapOf("gl" to "EN", "hl" to "en")
        ))
    }

    private fun Element?.extractPostId(): String {
        return this
                ?.attr("href")
                ?.substringAfter("/watch?v=")
                .orEmpty()
    }

    private fun Element?.extractPostCaption(): String {
        return this
                ?.attr("title")
                .orEmpty()
    }

    private fun Element?.extractPostViewsCount(): Int? {
        return this
                ?.getFirstElementByClass("yt-lockup-meta-info")
                ?.getElementsByTag("li")
                ?.getOrNull(1)
                ?.wholeText()
                ?.replace("views", "")
                ?.replace(",", "")
                ?.trim()
                ?.toIntOrNull()
    }

    private fun Element?.extractPostPublishDate(): Long? {
        return this
                ?.getFirstElementByClass("yt-lockup-meta-info")
                ?.getElementsByTag("li")
                ?.getOrNull(0)
                ?.wholeText()
                ?.let {
                    val now = currentUnixTimestamp()

                    val amount = it.split(" ")
                            .firstOrNull()
                            ?.toIntOrNull()
                            ?: 1

                    val temporalAmount: TemporalAmount = when {
                        it.contains("moment", ignoreCase = true) -> Duration.ofMillis(amount.toLong())
                        it.contains("second", ignoreCase = true) -> Duration.ofSeconds(amount.toLong())
                        it.contains("minute", ignoreCase = true) -> Duration.ofMinutes(amount.toLong())
                        it.contains("hour", ignoreCase = true) -> Duration.ofHours(amount.toLong())
                        it.contains("day", ignoreCase = true) -> Duration.ofDays(amount.toLong())
                        it.contains("week", ignoreCase = true) -> Period.ofWeeks(amount)
                        it.contains("month", ignoreCase = true) -> Period.ofMonths(amount)
                        it.contains("year", ignoreCase = true) -> Period.ofYears(amount)
                        else -> ZERO
                    }
                    val millisAgo = when (temporalAmount) {
                        is Duration -> temporalAmount.toMillis()
                        is Period -> Duration.ofDays(temporalAmount.get(DAYS)).toMillis()
                        is ChronoPeriod -> Duration.ofDays(temporalAmount.get(DAYS)).toMillis()
                        else -> 0
                    }
                    now - (millisAgo / 1000)
                }
    }

    private fun Element?.extractPostVideos(): List<Media> {
        return listOf(Video(
                url = this
                        ?.getFirstElementByClass("yt-uix-tile-link")
                        ?.attr("href")
                        ?.let { "$baseUrl${it}" }
                        .orEmpty(),
                duration = this
                        ?.getFirstElementByClass("video-time")
                        ?.wholeText()
                        ?.trim()
                        ?.split(":")
                        ?.map { it.toLongOrNull() }
                        ?.run {
                            val hours = getOrNull(0) ?: 0L
                            val minutes = getOrNull(1) ?: 0L
                            val seconds = getOrNull(2) ?: 0L

                            Duration.ofSeconds(seconds) + Duration.ofMinutes(minutes) + Duration.ofHours(hours)
                        }
        ))
    }

    private fun Document.extractPageNick(): String? {
        return this
                .getFirstElementByClass("channel-header-profile-image-container")
                ?.attr("href")
                ?.removeSuffix("/")
                ?.substringAfterLast("/")
    }

    private fun Document.extractPageName(): String? {
        return this
                .getFirstElementByClass("branded-page-header-title-link")
                ?.attr("title")
    }

    private fun Document.extractPageDescription(): String? {
        return this
                .getFirstElementByAttributeValue("name", "description")
                ?.attr("content")
    }

    private fun Document.extractFollowersCount(): Int? {
        return this
                .getFirstElementByClass("yt-subscription-button-subscriber-count-branded-horizontal")
                ?.wholeText()
                ?.let {
                    when {
                        it.endsWith("K") -> it.replace("K", "").replace(".", "").toIntOrNull()?.times(1_000)
                        it.endsWith("M", ignoreCase = true) -> it.replace("M", "").replace(".", "").toIntOrNull()?.times(1_000_000)
                        it.endsWith("B", ignoreCase = true) -> it.replace("B", "").replace(".", "").toIntOrNull()?.times(1_000_000_000)
                        else -> it.replace(".", "").toIntOrNull()
                    }
                }
    }

    private fun Document.extractPageAvatarsMap(): Map<MediaSize, Image> {
        return singleImageMap(url = this
                .getFirstElementByAttributeValue("rel", "image_src")
                ?.attr("href")
        )
    }

    private fun Document.extractPageCoversMap(): Map<MediaSize, Image> {
        val urls = this
                .getElementById("gh-banner")
                ?.html()
                ?.split("\n")
                ?.filter { it.contains("background-image") }
                ?.map {
                    val link = it.substringAfter("url(", "").substringBeforeLast(");", "")
                    "https:${link}"
                }
                .orEmpty()

        return mapOf(
                SMALL to urls.getOrElse(0) { "" }.toImage(),
                MEDIUM to urls.getOrElse(1) { "" }.toImage(),
                LARGE to urls.getOrElse(2) { "" }.toImage()
        )
    }
}