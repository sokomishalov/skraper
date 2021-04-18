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
    "DuplicatedCode"
)

package ru.sokomishalov.skraper.provider.youtube

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jsoup.nodes.Document
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.client.HttpRequest
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.client.fetchDocument
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.internal.iterable.emitThis
import ru.sokomishalov.skraper.internal.net.host
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.internal.serialization.getByPath
import ru.sokomishalov.skraper.internal.serialization.getInt
import ru.sokomishalov.skraper.internal.serialization.getString
import ru.sokomishalov.skraper.internal.serialization.readJsonNodes
import ru.sokomishalov.skraper.model.*
import java.time.Duration
import java.time.Duration.ZERO
import java.time.Instant
import java.time.Period
import java.time.chrono.ChronoPeriod
import java.time.temporal.ChronoUnit.DAYS
import java.time.temporal.TemporalAmount

open class YoutubeSkraper @JvmOverloads constructor(
    override val client: SkraperClient = DefaultBlockingSkraperClient,
    override val baseUrl: String = "https://www.youtube.com"
) : Skraper {

    override fun getPosts(path: String): Flow<Post> = flow {
        val page = getUserPage(path = path)

        val jsonMetadata = page?.readJsonMetadata()

        val videoItems = jsonMetadata
            ?.findParents("gridVideoRenderer")
            ?.map { it["gridVideoRenderer"] }
            .orEmpty()

        videoItems.emitThis(this) {
            Post(
                id = getString("videoId").orEmpty(),
                text = getString("title.runs.0.text"),
                viewsCount = getString("viewCountText.simpleText")?.substringBefore(" ")?.toIntOrNull(),
                publishedAt = getString("publishedTimeText")?.extractTimeAgo(),
                media = extractVideos()
            )
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val page = getUserPage(path = path)
        val jsonMetadata = page?.readJsonMetadata()

        return jsonMetadata?.run {
            PageInfo(
                nick = getString("metadata.channelMetadataRenderer.vanityChannelUrl")?.substringAfter("/user/"),
                name = getString("metadata.channelMetadataRenderer.title"),
                description = getString("metadata.channelMetadataRenderer.description"),
                followersCount = getString("header.c4TabbedHeaderRenderer.subscriberCountText.runs.0.text")?.extractAmount(),
                avatar = getByPath("header.c4TabbedHeaderRenderer.avatar.thumbnails").extractImage(),
                cover = getByPath("header.c4TabbedHeaderRenderer.banner.thumbnails").extractImage()
            )
        }
    }

    override fun supports(url: String): Boolean {
        return setOf("youtube.com", "youtu.be")
            .any { url.host.removePrefix("www.") in it }
    }

    override suspend fun resolve(media: Media): Media {
        return when (media) {
            is Video -> runCatching {
                YoutubeVideoResolver(client = client, baseUrl = baseUrl).getVideo(media)
            }.getOrNull() ?: media
            else -> media
        }
    }

    private suspend fun getUserPage(path: String): Document? {
        return client.fetchDocument(
            HttpRequest(
                url = baseUrl.buildFullURL(
                    path = path,
                    queryParams = mapOf("gl" to "EN", "hl" to "en")
                ),
                headers = emptyMap()
            )
        )
    }

    private fun Document.readJsonMetadata(): JsonNode? {
        return this
            .getElementsByTag("script")
            .mapNotNull { it.html() }
            .find { "ytInitialData" in it }
            ?.substringAfter("var ytInitialData = ")
            .readJsonNodes()
    }

    private fun JsonNode.extractVideos(): List<Video> {
        return listOf(Video(
            url = baseUrl + getString("navigationEndpoint.commandMetadata.webCommandMetadata.url"),
            duration = getString("thumbnailOverlays.0.thumbnailOverlayTimeStatusRenderer.text.simpleText")?.extractDuration(),
            thumbnail = getByPath("thumbnail.thumbnails")?.lastOrNull()?.run {
                Image(
                    url = getString("url").orEmpty(),
                    aspectRatio = getInt("width") / getInt("height")
                )
            }
        ))
    }

    private fun JsonNode?.extractImage(): Image? {
        return this
            ?.toList()
            ?.takeLast(3)
            ?.map { node ->
                val url = node.getString("url")?.let { if (it.startsWith("//")) "https:${it}" else it }.orEmpty()
                val ratio = node.getInt("width") / node.getInt("height")

                Image(url = url, aspectRatio = ratio)
            }
            ?.firstOrNull()
    }

    private fun String.extractTimeAgo(): Instant? {
        val nowMillis = System.currentTimeMillis()

        val amount = split(" ")
            .firstOrNull()
            ?.toIntOrNull()
            ?: 1

        val temporalAmount: TemporalAmount = when {
            contains("moment", ignoreCase = true) -> Duration.ofMillis(amount.toLong())
            contains("second", ignoreCase = true) -> Duration.ofSeconds(amount.toLong())
            contains("minute", ignoreCase = true) -> Duration.ofMinutes(amount.toLong())
            contains("hour", ignoreCase = true) -> Duration.ofHours(amount.toLong())
            contains("day", ignoreCase = true) -> Duration.ofDays(amount.toLong())
            contains("week", ignoreCase = true) -> Period.ofWeeks(amount)
            contains("month", ignoreCase = true) -> Period.ofMonths(amount)
            contains("year", ignoreCase = true) -> Period.ofYears(amount)
            else -> ZERO
        }
        val millisAgo = when (temporalAmount) {
            is Duration -> temporalAmount.toMillis()
            is Period -> Duration.ofDays(temporalAmount.get(DAYS)).toMillis()
            is ChronoPeriod -> Duration.ofDays(temporalAmount.get(DAYS)).toMillis()
            else -> 0
        }

        return Instant.ofEpochMilli(nowMillis - millisAgo)
    }

    private fun String.extractDuration(): Duration {
        return this
            .trim()
            .split(":")
            .map { it.toLongOrNull() }
            .run {
                val hours = getOrNull(0) ?: 0L
                val minutes = getOrNull(1) ?: 0L
                val seconds = getOrNull(2) ?: 0L

                Duration.ofSeconds(seconds) + Duration.ofMinutes(minutes) + Duration.ofHours(hours)
            }
    }

    private fun String.extractAmount(): Int? {
        return substringBefore(" ")
            .trim()
            .run {
                when {
                    endsWith("K", ignoreCase = true) -> replace("K", "").substringBeforeLast(".").toIntOrNull()?.times(1_000)
                    endsWith("M", ignoreCase = true) -> replace("M", "").substringBeforeLast(".").toIntOrNull()?.times(1_000_000)
                    endsWith("B", ignoreCase = true) -> replace("B", "").substringBeforeLast(".").toIntOrNull()?.times(1_000_000_000)
                    else -> substringBeforeLast(".").toIntOrNull()
                }
            }
    }
}