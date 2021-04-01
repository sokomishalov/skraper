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
import org.jsoup.nodes.Document
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.client.HttpRequest
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.client.fetchDocument
import ru.sokomishalov.skraper.client.fetchOpenGraphMedia
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByTag
import ru.sokomishalov.skraper.internal.serialization.getByPath
import ru.sokomishalov.skraper.internal.serialization.getFirstByPath
import ru.sokomishalov.skraper.internal.serialization.getString
import ru.sokomishalov.skraper.internal.serialization.readJsonNodes
import ru.sokomishalov.skraper.model.*

/**
 * @author sokomishalov
 */
open class IFunnySkraper @JvmOverloads constructor(
    override val client: SkraperClient = DefaultBlockingSkraperClient,
    override val baseUrl: URLString = "https://ifunny.co"
) : Skraper {

    override suspend fun getPosts(path: String, limit: Int): List<Post> {
        val page = getPage(path = path)

        val posts = page
            ?.getElementsByClass("stream__item")
            ?.take(limit)
            .orEmpty()

        return posts.mapNotNull {
            val a = it.getFirstElementByTag("a")

            val img = a?.getFirstElementByTag("img")
            val link = a?.attr("href").orEmpty()

            val isVideo = "video" in link || "gif" in link

            val aspectRatio = it
                .attr("data-ratio")
                .toDoubleOrNull()
                ?.let { r -> 1.0 / r }

            Post(
                id = link.substringBeforeLast("?").substringAfterLast("/"),
                media = listOf(
                    when {
                        isVideo -> Video(
                            url = "${baseUrl}${link}",
                            aspectRatio = aspectRatio
                        )
                        else -> Image(
                            url = img?.attr("data-src").orEmpty(),
                            aspectRatio = aspectRatio
                        )
                    }
                )
            )
        }
    }

    override suspend fun resolve(media: Media): Media {
        return client.fetchOpenGraphMedia(media)
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val page = getPage(path = path)

        val json = page.extractInitialJson()

        return json?.run {
            PageInfo(
                nick = getString("nick"),
                description = getString("about"),
                avatar = getFirstByPath("photo.thumb.large_url", "photo.thumb.large_url", "photo.thumb.large_url", "photo.url")?.asText()?.toImage(),
                cover = getString("coverUrl")?.toImage()
            )
        }
    }

    private fun Document?.extractInitialJson(): JsonNode? {
        val textJson = this
            ?.getElementsByTag("script")
            ?.find { "__INITIAL_STATE__" in it.html() }
            ?.html()
            ?.removePrefix("window.__INITIAL_STATE__ = ")
            ?.removeSuffix(";")

        return textJson?.run { readJsonNodes() }?.getByPath("user.data")
    }


    private suspend fun getPage(path: String): Document? {
        return client.fetchDocument(HttpRequest(url = baseUrl.buildFullURL(path = path)))
    }
}