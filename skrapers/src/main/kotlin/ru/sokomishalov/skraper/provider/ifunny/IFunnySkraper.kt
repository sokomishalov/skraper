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
package ru.sokomishalov.skraper.provider.ifunny

import org.jsoup.nodes.Document
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_ASPECT_RATIO
import ru.sokomishalov.skraper.internal.jsoup.getSingleElementByTag
import ru.sokomishalov.skraper.internal.url.uriCleanUp
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.AttachmentType.VIDEO
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.Post

/**
 * @author sokomishalov
 */
class IFunnySkraper @JvmOverloads constructor(
        override val client: SkraperClient = DefaultBlockingSkraperClient
) : Skraper {

    override val baseUrl: String = "https://ifunny.co"

    override suspend fun getLatestPosts(uri: String, limit: Int): List<Post> {
        val document = getTopicPage(uri)

        val posts = document
                ?.getElementsByClass("stream__item")
                ?.take(limit)
                .orEmpty()

        return posts.mapNotNull {
            val a = it.getSingleElementByTag("a")

            val img = a.getSingleElementByTag("img")
            val link = a.attr("href")

            val video = "video" in link || "gif" in link
            Post(
                    id = link.substringBeforeLast("?").substringAfterLast("/"),
                    attachments = listOf(Attachment(
                            url = when {
                                video -> "${baseUrl}${link}"
                                else -> img.attr("data-src")
                            },
                            type = when {
                                video -> VIDEO
                                else -> IMAGE
                            },
                            aspectRatio = it
                                    .attr("data-ratio")
                                    .toDoubleOrNull()
                                    ?.let { r -> 1.0 / r }
                                    ?: DEFAULT_POSTS_ASPECT_RATIO
                    ))
            )
        }
    }

    override suspend fun getPageLogoUrl(uri: String, imageSize: ImageSize): String? {
        return getLogoUrl(imageSize)
    }

    private suspend fun getTopicPage(uri: String): Document? {
        return client.fetchDocument("${baseUrl}/${uri.uriCleanUp()}")
    }
}