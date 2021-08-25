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
package ru.sokomishalov.skraper.provider.ifunny

import com.fasterxml.jackson.databind.JsonNode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jsoup.nodes.Document
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.Skrapers
import ru.sokomishalov.skraper.client.HttpRequest
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.client.fetchDocument
import ru.sokomishalov.skraper.client.fetchOpenGraphMedia
import ru.sokomishalov.skraper.internal.iterable.emitBatch
import ru.sokomishalov.skraper.internal.net.host
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.internal.serialization.*
import ru.sokomishalov.skraper.model.*
import java.time.Instant

/**
 * @author sokomishalov
 */
open class IFunnySkraper @JvmOverloads constructor(
    override val client: SkraperClient = Skrapers.client
) : Skraper {

    override fun getPosts(uri: String): Flow<Post> = flow {
        var nextPath = when {
            uri.startsWith("/user") -> uri
            else -> uri.removeSuffix("/") + "/page"
        }

        while (true) {
            val page = getPage(path = nextPath)

            val rawPosts = page?.extractPageJson()?.getByPath("feed.items")?.toList()

            if (rawPosts.isNullOrEmpty()) break

            emitBatch(rawPosts) {
                val url = getString("url").orEmpty()
                val aspectRatio = getDouble("size.w") / getDouble("size.h")
                Post(
                    id = getString("id").orEmpty(),
                    text = getString("title"),
                    publishedAt = getLong("published")?.let { Instant.ofEpochSecond(it) },
                    statistics = PostStatistics(
                        likes = getInt("smiles"),
                        comments = getInt("comments"),
                    ),
                    media = listOf(
                        when (getString("type")) {
                            "picture" -> Image(
                                url = url,
                                aspectRatio = aspectRatio
                            )
                            else -> Video(
                                url = url,
                                aspectRatio = aspectRatio
                            )
                        }
                    )
                )
            }

            val nextPageNumber = nextPath.substringAfterLast("/page").toIntOrNull()?.plus(1) ?: break
            nextPath = nextPath.substringBeforeLast("/page") + "/page${nextPageNumber}"
        }
    }

    override fun supports(media: Media): Boolean {
        return "ifunny.co" in media.url.host
    }

    override suspend fun resolve(media: Media): Media {
        return client.fetchOpenGraphMedia(media)
    }

    override suspend fun getPageInfo(uri: String): PageInfo? {
        val page = getPage(path = uri)

        val json = page.extractPageJson()?.getByPath("user.data")

        return json?.run {
            PageInfo(
                nick = getString("nick"),
                description = getString("about"),
                statistics = PageStatistics(
                    followers = getInt("subscribers"),
                    following = getInt("subscriptions"),
                ),
                avatar = getFirstByPath("avatar.l", "avatar.m", "avatar.s", "avatar.url")?.asText()?.toImage(),
                cover = getString("coverUrl")?.
                toImage()
            )
        }
    }

    private fun Document?.extractPageJson(): JsonNode? {
        val textJson = this
            ?.getElementsByTag("script")
            ?.find { "__INITIAL_STATE__" in it.html() }
            ?.html()
            ?.removePrefix("window.__INITIAL_STATE__=")
            ?.removeSuffix(";")

        return textJson?.run { readJsonNodes() }
    }


    private suspend fun getPage(path: String): Document? {
        return client.fetchDocument(HttpRequest(url = BASE_URL.buildFullURL(path = path)))
    }

    companion object {
        const val BASE_URL: String = "https://ifunny.co"
    }
}