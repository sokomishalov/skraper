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
@file:Suppress("MemberVisibilityCanBePrivate")

package ru.sokomishalov.skraper.provider.tumblr

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
import ru.sokomishalov.skraper.internal.jsoup.*
import ru.sokomishalov.skraper.internal.net.host
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.model.*

open class TumblrSkraper @JvmOverloads constructor(
    override val client: SkraperClient = Skrapers.client
) : Skraper {

    override fun getPosts(path: String): Flow<Post> = flow {
        val page = getNonUserPage(path = path)

        val rawPosts = page?.getElementsByTag("article").orEmpty()

        emitBatch(rawPosts) {
            Post(
                id = getFirstElementByAttribute("data-login-wall-id")
                    ?.attr("data-login-wall-id")
                    .orEmpty(),
                text = getFirstElementByAttributeValue("data-login-wall-type", "tags")
                    ?.parent()
                    ?.wholeText()
                    .orEmpty(),
                media = getElementsByTag("figure").mapNotNull { f ->
                    val video = f.getFirstElementByTag("video")
                    val img = f.getFirstElementByTag("img")

                    when {
                        video != null -> video.getFirstElementByTag("source")?.attr("src")?.toVideo()
                        img != null -> img.attr("srcset").split(", ").lastOrNull()?.substringBefore(" ")?.toImage()
                        else -> null
                    }
                }
            )
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val page = getNonUserPage(path = path)
        val metaProperties = page.getMetaPropertyMap()

        return PageInfo(
            nick = metaProperties["profile:username"].orEmpty(),
            name = metaProperties["og:title"].orEmpty(),
            description = metaProperties["og:description"].orEmpty(),
            avatar = run {
                Image(
                    url = metaProperties["og:image"].orEmpty(),
                    aspectRatio = metaProperties["og:image:width"]?.toDoubleOrNull() / metaProperties["og:image:height"]?.toDoubleOrNull()
                )
            },
            cover = page
                ?.getFirstElementByAttributeValue("data-login-wall-type", "blogView")
                ?.getFirstElementByTag("img")
                ?.attr("src")
                ?.toImage()
        )
    }

    override fun supports(url: String): Boolean {
        return "tumblr.com" in url.host
    }

    override suspend fun resolve(media: Media): Media {
        return client.fetchOpenGraphMedia(media)
    }

    internal suspend fun getUserPage(username: String): Document? {
        return client.fetchDocument(HttpRequest(url = BASE_URL.replace("://", "://${username}.")))
    }

    private suspend fun getNonUserPage(path: String): Document? = when {
        "/dashboard/blog/" in path -> {
            val username = path.substringAfter("/dashboard/blog/").substringBefore("/")
            getUserPage(username = username)
        }
        "/blog/view/" in path -> {
            val username = path.substringAfter("/blog/view/").substringBefore("/")
            getUserPage(username = username)
        }
        else -> client.fetchDocument(HttpRequest(url = BASE_URL.buildFullURL(path = path)))
    }

    companion object {
        const val BASE_URL: String = "https://tumblr.com"
    }
}