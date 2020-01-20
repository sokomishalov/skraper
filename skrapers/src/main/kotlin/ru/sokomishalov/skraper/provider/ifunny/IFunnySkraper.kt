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

import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchAspectRatio
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.internal.jsoup.getSingleElementByClass
import ru.sokomishalov.skraper.internal.jsoup.getSingleElementByTag
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.Post

/**
 * @author sokomishalov
 */
class IFunnySkraper @JvmOverloads constructor(
        override val client: SkraperClient = DefaultBlockingSkraperClient()
) : Skraper {

    companion object {
        private const val IFUNNY_URL = "https://ifunny.co"
    }

    override suspend fun getLatestPosts(uri: String, limit: Int, fetchAspectRatio: Boolean): List<Post> {
        val document = client.fetchDocument("${IFUNNY_URL}/${uri}")

        val posts = document
                ?.getSingleElementByClass("feed__list")
                ?.getElementsByClass("stream__item")
                ?.take(limit)
                .orEmpty()

        return posts
                .map {
                    val a = it.getSingleElementByTag("a")

                    val img = a.getSingleElementByTag("img")
                    val link = a.attr("href")

                    // videos and gifs cannot be scraped :(
                    if ("video" in link || "gif" in link) return@map null

                    Post(
                            id = link.convertUriToId(),
                            attachments = listOf(Attachment(
                                    url = img.attr("data-src"),
                                    type = IMAGE,
                                    aspectRatio = it
                                            .attr("data-ratio")
                                            .toDoubleOrNull()
                                            ?.let { 1.div(it) }
                                            ?: client.fetchAspectRatio(url = img.attr("data-src"), fetchAspectRatio = fetchAspectRatio)
                            ))
                    )
                }
                .filterNotNull()
    }

    override suspend fun getPageLogoUrl(uri: String, imageSize: ImageSize): String? {
        return client.fetchDocument("${IFUNNY_URL}/${uri}")
                ?.getElementsByTag("meta")
                ?.find { it.attr("property") == "og:image" }
                ?.attr("content")
    }

    private fun String.convertUriToId(): String {
        return this
                .substringBeforeLast("?")
                .replaceFirst("/", "")
    }
}