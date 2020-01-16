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
import ru.sokomishalov.skraper.internal.model.Attachment
import ru.sokomishalov.skraper.internal.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.internal.model.Post
import ru.sokomishalov.skraper.internal.model.ProviderChannel
import ru.sokomishalov.skraper.internal.util.http.getImageAspectRatio
import ru.sokomishalov.skraper.internal.util.jsoup.fetchDocument
import ru.sokomishalov.skraper.internal.util.jsoup.getSingleElementByClass
import ru.sokomishalov.skraper.internal.util.jsoup.removeLinks
import java.util.*


/**
 * @author sokomishalov
 */
class TwitterSkraper : Skraper {

    companion object {
        private const val TWITTER_URL = "https://twitter.com"
    }

    override suspend fun fetchPosts(channel: ProviderChannel, limit: Int): List<Post> {
        val webPage = fetchDocument("$TWITTER_URL/${channel.uri}")

        val posts = webPage
                ?.body()
                ?.getElementById("stream-items-id")
                ?.getElementsByClass("stream-item")
                ?.take(limit)
                ?.map { it.getSingleElementByClass("tweet") }
                .orEmpty()

        return posts.map {
            Post(
                    id = extractIdFromTweet(it),
                    caption = extractCaptionFromTweet(it),
                    publishedAt = extractPublishedAtFromTweet(it),
                    attachments = extractAttachmentsFromTweet(it)
            )
        }
    }

    override suspend fun getChannelLogoUrl(channel: ProviderChannel): String? {
        return fetchDocument("$TWITTER_URL/${channel.uri}")
                ?.body()
                ?.getSingleElementByClass("ProfileAvatar-image")
                ?.attr("src")
    }

    private fun extractIdFromTweet(tweet: Element): String {
        return tweet
                .getSingleElementByClass("js-stream-tweet")
                .attr("data-tweet-id")
    }

    private fun extractCaptionFromTweet(tweet: Element): String? {
        return tweet
                .getSingleElementByClass("tweet-text")
                .removeLinks()
    }

    private fun extractPublishedAtFromTweet(tweet: Element): Date {
        return runCatching {
            tweet
                    .getSingleElementByClass("js-short-timestamp")
                    .attr("data-time-ms")
                    .toLong()
                    .let { Date(it) }
        }.getOrElse { Date(0) }
    }

    private suspend fun extractAttachmentsFromTweet(tweet: Element): List<Attachment> {
        return tweet
                .getElementsByClass("AdaptiveMedia-photoContainer")
                ?.map { element ->
                    element.attr("data-image-url").let {
                        Attachment(
                                url = it,
                                type = IMAGE,
                                aspectRatio = getImageAspectRatio(it)
                        )
                    }
                }
                ?: emptyList()
    }
}