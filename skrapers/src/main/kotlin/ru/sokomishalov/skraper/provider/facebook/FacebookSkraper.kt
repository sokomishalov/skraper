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
package ru.sokomishalov.skraper.provider.facebook

import com.fasterxml.jackson.databind.JsonNode
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_ASPECT_RATIO
import ru.sokomishalov.skraper.internal.jsoup.getSingleElementByAttributeOrNull
import ru.sokomishalov.skraper.internal.jsoup.getSingleElementByClassOrNull
import ru.sokomishalov.skraper.internal.jsoup.getSingleElementByTagOrNull
import ru.sokomishalov.skraper.internal.serialization.aReadJsonNodes
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.AttachmentType.VIDEO
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.Post
import kotlin.text.Charsets.UTF_8


/**
 * @author sokomishalov
 */
class FacebookSkraper(
        override val client: SkraperClient = DefaultBlockingSkraperClient,
        override val baseUrl: String = "https://facebook.com"
) : Skraper {

    override suspend fun getPosts(path: String, limit: Int): List<Post> {
        val document = getPage(path = path)

        val elements = document.extractPosts(limit)
        val jsonData = document.extractJsonData()
        val metaInfoJsonMap = jsonData.prepareMetaInfoMap()

        return elements.map {
            val id = it.extractId()
            val node = metaInfoJsonMap[id]

            Post(
                    id = id,
                    text = it.extractText(),
                    publishedAt = it.extractPublishDateTime(),
                    rating = node.extractReactionCount(),
                    commentsCount = node.extractCommentsCount(),
                    attachments = it.extractAttachments()
            )
        }
    }

    override suspend fun getLogoUrl(path: String, imageSize: ImageSize): String? {
        val document = getPage(path = path)

        return document
                ?.getElementsByAttributeValue("property", "og:image")
                ?.firstOrNull()
                ?.attr("content")
    }


    private suspend fun getPage(path: String): Document? = client.fetchDocument("$baseUrl$path")

    private fun JsonNode?.prepareMetaInfoMap(): Map<String, JsonNode> {
        return this
                ?.get("pre_display_requires")
                ?.toList()
                ?.map { it.findPath("__bbox") }
                ?.mapNotNull { it?.get("result")?.get("data")?.get("feedback") }
                ?.map { it["share_fbid"].asText() to it }
                ?.toMap()
                .orEmpty()
    }

    private suspend fun Document?.extractJsonData(): JsonNode? {
        val infoJsonPrefix = "new (require(\"ServerJS\"))().handle("
        val infoJsonSuffix = ");"

        return this
                ?.getElementsByTag("script")
                ?.find { s -> s.html().startsWith(infoJsonPrefix) }
                ?.run {
                    html()
                            .removePrefix(infoJsonPrefix)
                            .removeSuffix(infoJsonSuffix)
                            .toByteArray(UTF_8)
                            .aReadJsonNodes()
                }
    }

    private fun Document?.extractPosts(limit: Int): List<Element> {
        return this
                ?.getElementsByClass("userContentWrapper")
                ?.toList()
                ?.take(limit)
                .orEmpty()
    }

    private fun Element.extractId(): String {
        return getElementsByAttributeValue("name", "ft_ent_identifier")
                ?.firstOrNull()
                ?.attr("value")
                .orEmpty()
    }

    private fun Element.extractText(): String? {
        return getSingleElementByClassOrNull("userContent")
                ?.getSingleElementByTagOrNull("p")
                ?.wholeText()
                ?.toString()
    }

    private fun Element.extractPublishDateTime(): Long? {
        return getSingleElementByAttributeOrNull("data-utime")
                ?.attr("data-utime")
                ?.toLongOrNull()
                ?.times(1000)
    }

    private fun JsonNode?.extractReactionCount(): Int? {
        return this
                ?.get("reaction_count")
                ?.get("count")
                ?.asInt()
    }

    private fun JsonNode?.extractCommentsCount(): Int? {
        return this
                ?.get("display_comments_count")
                ?.get("count")
                ?.asInt()
    }

    private fun Element.extractAttachments(): List<Attachment> {
        val videoElement = getSingleElementByTagOrNull("video")

        return when {
            videoElement != null -> listOf(Attachment(
                    type = VIDEO,
                    url = getElementsByAttributeValueContaining("id", "feed_subtitle")
                            .firstOrNull()
                            ?.getSingleElementByTagOrNull("a")
                            ?.attr("href")
                            ?.let { "${baseUrl}${it}" }
                            .orEmpty(),
                    aspectRatio = videoElement
                            .attr("data-original-aspect-ratio")
                            .toDoubleOrNull()
                            ?: DEFAULT_POSTS_ASPECT_RATIO
            ))

            else -> getSingleElementByClassOrNull("uiScaledImageContainer")
                    ?.getSingleElementByTagOrNull("img")
                    ?.run {
                        val url = attr("src")
                        val width = attr("width").toDoubleOrNull()
                        val height = attr("height").toDoubleOrNull()

                        listOf(Attachment(
                                type = IMAGE,
                                url = url,
                                aspectRatio = when {
                                    width != null && height != null -> width / height
                                    else -> DEFAULT_POSTS_ASPECT_RATIO
                                }

                        ))
                    }
                    ?: emptyList()
        }
    }
}