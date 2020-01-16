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
import ru.sokomishalov.skraper.internal.util.jsoup.fetchDocument
import ru.sokomishalov.skraper.internal.util.jsoup.getSingleElementByClass
import ru.sokomishalov.skraper.internal.util.jsoup.getSingleElementByTag
import ru.sokomishalov.skraper.internal.util.time.mockDate
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.Post
import ru.sokomishalov.skraper.model.ProviderChannel

/**
 * @author sokomishalov
 */
class IFunnySkraper : Skraper {

    companion object {
        private const val IFUNNY_URL = "https://ifunny.co"
    }

    override suspend fun getLatestPosts(channel: ProviderChannel, limit: Int): List<Post> {
        val document = fetchDocument("${IFUNNY_URL}/${channel.uri}")

        val posts = document
                ?.getSingleElementByClass("feed__list")
                ?.getElementsByClass("stream__item")
                ?.take(limit)
                .orEmpty()

        return posts
                .mapIndexed { i, it ->
                    val a = it.getSingleElementByTag("a")

                    val img = a.getSingleElementByTag("img")
                    val link = a.attr("href")

                    // videos and gifs cannot be scraped :(
                    if ("video" in link || "gif" in link) return@mapIndexed null

                    Post(
                            id = link.convertUriToId(),
                            publishedAt = mockDate(i),
                            attachments = listOf(Attachment(
                                    url = img.attr("data-src"),
                                    type = IMAGE,
                                    aspectRatio = it.attr("data-ratio").toDoubleOrNull()?.let { 1.div(it) }
                            ))
                    )
                }
                .filterNotNull()
    }

    override suspend fun getChannelLogoUrl(channel: ProviderChannel): String? {
        return fetchDocument("${IFUNNY_URL}/${channel.uri}")
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