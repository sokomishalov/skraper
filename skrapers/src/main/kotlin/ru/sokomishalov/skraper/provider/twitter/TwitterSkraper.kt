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
package ru.sokomishalov.skraper.provider.twitter

import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchAspectRatio
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.internal.jsoup.getSingleElementByClass
import ru.sokomishalov.skraper.internal.jsoup.removeLinks
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.Post


/**
 * @author sokomishalov
 */
class TwitterSkraper @JvmOverloads constructor(
        override val client: SkraperClient = DefaultBlockingSkraperClient()
) : Skraper {

    companion object {
        private const val TWITTER_URL = "https://twitter.com"
    }

    override suspend fun getLatestPosts(uri: String, limit: Int, fetchAspectRatio: Boolean): List<Post> {
        val webPage = client.fetchDocument("$TWITTER_URL/${uri}")

        val posts = webPage
                ?.body()
                ?.getElementById("stream-items-id")
                ?.getElementsByClass("stream-item")
                ?.take(limit)
                ?.map { it.getSingleElementByClass("tweet") }
                .orEmpty()

        return posts.map {
            Post(
                    id = it.extractIdFromTweet(),
                    caption = it.extractCaptionFromTweet(),
                    publishTimestamp = it.extractPublishedAtFromTweet(),
                    attachments = it.extractAttachmentsFromTweet(fetchAspectRatio = fetchAspectRatio)
            )
        }
    }

    override suspend fun getPageLogoUrl(uri: String, imageSize: ImageSize): String? {
        return client.fetchDocument("$TWITTER_URL/${uri}")
                ?.body()
                ?.getSingleElementByClass("ProfileAvatar-image")
                ?.attr("src")
    }

    private fun Element.extractIdFromTweet(): String {
        return getSingleElementByClass("js-stream-tweet")
                .attr("data-tweet-id")
    }

    private fun Element.extractCaptionFromTweet(): String? {
        return getSingleElementByClass("tweet-text")
                .removeLinks()
    }

    private fun Element.extractPublishedAtFromTweet(): Long {
        return getSingleElementByClass("js-short-timestamp")
                .attr("data-time-ms")
                .toLong()
    }

    private suspend fun Element.extractAttachmentsFromTweet(fetchAspectRatio: Boolean): List<Attachment> {
        return getElementsByClass("AdaptiveMedia-photoContainer")
                ?.map { element ->
                    Attachment(
                            url = element.attr("data-image-url"),
                            type = IMAGE,
                            aspectRatio = client.fetchAspectRatio(element.attr("data-image-url"), fetchAspectRatio = fetchAspectRatio)
                    )
                }
                ?: emptyList()
    }
}