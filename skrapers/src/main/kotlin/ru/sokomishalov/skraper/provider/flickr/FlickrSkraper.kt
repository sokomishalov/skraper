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
package ru.sokomishalov.skraper.provider.flickr

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.Skrapers
import ru.sokomishalov.skraper.client.HttpRequest
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.client.fetchDocument
import ru.sokomishalov.skraper.client.fetchOpenGraphMedia
import ru.sokomishalov.skraper.internal.iterable.emitBatch
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByClass
import ru.sokomishalov.skraper.internal.net.host
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.internal.serialization.*
import ru.sokomishalov.skraper.internal.string.unescapeHtml
import ru.sokomishalov.skraper.internal.string.unescapeUrl
import ru.sokomishalov.skraper.model.*
import java.time.Instant


/**
 * @author sokomishalov
 */
open class FlickrSkraper @JvmOverloads constructor(
    override val client: SkraperClient = Skrapers.client
) : Skraper {

    override fun getPosts(path: String): Flow<Post> = flow {
        val page = getPage(path = path)

        val jsonPosts = page
            .parseModelJson()
            ?.findPath("_data")
            ?.toList()
            .orEmpty()

        emitBatch(jsonPosts) {
            Post(
                id = getString("data.id").orEmpty(),
                text = run {
                    val title = getByPath("data.title").unescapeNode()
                    val description = getByPath("data.description").unescapeNode()

                    "${title}\n\n${description}"
                },
                publishedAt = getLong("data.stats.data.datePosted")?.let { Instant.ofEpochSecond(it) },
                statistics = PostStatistics(
                    likes = getByPath("data.engagement.data.faveCount")?.asInt(),
                    comments = getByPath("data.engagement.data.commentCount")?.asInt(),
                    views = getByPath("data.engagement.data.viewCount")?.asInt(),
                ),
                media = listOf(
                    Image(
                        url = getFirstByPath(
                            "data.sizes.data.l.data",
                            "data.sizes.data.m.data",
                            "data.sizes.data.s.data",
                        )?.getString("url")?.toURL().orEmpty(),
                        aspectRatio = getFirstByPath(
                            "data.sizes.data.l.data",
                            "data.sizes.data.m.data",
                            "sizes.data.s.data",
                        )?.run { getDouble("width") / getDouble("height") }
                    )
                )
            )
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val page = getPage(path = path)
        val json = page.parseModelJson()

        return json?.run {
            PageInfo(
                nick = getFirstByPath(
                    "photostream-models.0.data.owner.data.pathAlias",
                    "person-models.0.data.pathAlias"
                )?.unescapeNode(),
                name = getFirstByPath(
                    "photostream-models.0.data.owner.data.username",
                    "person-models.0.data.username"
                )?.unescapeNode(),
                description = getByPath("person-public-profile-models.0.data.profileDescriptionExpanded")
                    ?.unescapeNode()
                    ?.let { Jsoup.parse(it).wholeText() },
                statistics = PageStatistics(
                    followers = getInt("person-contacts-count-models.0.data.followerCount"),
                    following = getInt("person-contacts-count-models.0.data.followingCount"),
                    posts = getInt("person-profile-models.0.data.photoCount"),
                ),
                avatar = getFirstByPath(
                    "photostream-models.0.data.owner.data.buddyicon.data",
                    "person-models.0.data.buddyicon.data"
                )
                    ?.getFirstByPath("large", "medium", "small", "default")
                    ?.asText()
                    ?.toURL()
                    ?.toImage(),
                cover = getByPath("person-profile-models.0.data.coverPhotoUrls.data")
                    ?.getFirstByPath("h", "l", "s")
                    ?.asText()
                    ?.toURL()
                    ?.toImage()
            )
        }
    }

    override fun supports(url: String): Boolean {
        return "flickr.com" in url.host
    }

    override suspend fun resolve(media: Media): Media {
        return when (media) {
            is Image -> client.fetchOpenGraphMedia(media)
            else -> media
        }
    }

    private suspend fun getPage(path: String): Document? {
        return client.fetchDocument(HttpRequest(url = BASE_URL.buildFullURL(path = path)))
    }

    private fun Document?.parseModelJson(): JsonNode? {
        val fullJson = this
            ?.getFirstElementByClass("modelExport")
            ?.html()
            ?.substringAfter("Y.ClientApp.init(")
            ?.substringBefore(".then(function()")
            ?.substringBeforeLast(")")
            ?.replace("auth: auth,", "")
            ?.replace("reqId: reqId,", "")

        return runCatching {
            fullJson
                ?.readJsonNodes()
                ?.getByPath("modelExport.main")
        }.getOrNull()
    }

    private fun String.toURL(): String = let { "https:${it}" }

    private fun JsonNode?.unescapeNode(): String {
        return runCatching {
            this?.asText()?.unescapeUrl()?.unescapeHtml().orEmpty()
        }.getOrElse {
            this?.asText().orEmpty()
        }
    }

    companion object {
        const val BASE_URL: String = "https://flickr.com"
    }
}
