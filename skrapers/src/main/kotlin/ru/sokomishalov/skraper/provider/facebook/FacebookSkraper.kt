/**
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
package ru.sokomishalov.skraper.provider.facebook

import com.fasterxml.jackson.databind.JsonNode
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.fetchMediaWithOpenGraphMeta
import ru.sokomishalov.skraper.internal.jsoup.*
import ru.sokomishalov.skraper.internal.net.queryParams
import ru.sokomishalov.skraper.internal.number.div
import ru.sokomishalov.skraper.internal.serialization.getByPath
import ru.sokomishalov.skraper.internal.serialization.getInt
import ru.sokomishalov.skraper.internal.serialization.getString
import ru.sokomishalov.skraper.internal.serialization.readJsonNodes
import ru.sokomishalov.skraper.model.*


/**
 * @author sokomishalov
 */
class FacebookSkraper @JvmOverloads constructor(
        override val client: SkraperClient = DefaultBlockingSkraperClient,
        override val baseUrl: URLString = "https://facebook.com"
) : Skraper {

    override suspend fun getPosts(path: String, limit: Int): List<Post> {
        val postsPath = path.substringBefore("/posts") + "/posts"
        val page = getPage(path = postsPath)

        val posts = page.extractPosts(limit)
        val jsonData = page.extractJsonData()
        val metaInfoJsonMap = jsonData.prepareMetaInfoMap()
        return posts.map {
            val id = it.extractPostId()
            val metaInfoJson = metaInfoJsonMap[id]

            Post(
                    id = id,
                    text = it.extractPostText(),
                    additionalText = it.extractAdditionalText(),
                    publishedAt = it.extractPostPublishDateTime(),
                    rating = metaInfoJson?.extractPostReactionCount(),
                    commentsCount = metaInfoJson?.extractPostCommentsCount(),
                    viewsCount = metaInfoJson?.extractPostViewsCount(),
                    media = it.extractPostMediaItems()
            )
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val page = getPage(path = path)
        return page?.run {
            val isCommunity = getFirstElementByAttributeValue("data-key", "tab_community") != null

            when {
                isCommunity -> PageInfo(
                        nick = path.removePrefix("/").removePrefix("pg/").substringBefore("/"),
                        name = extractCommunityName(),
                        description = extractCommunityDescription(),
                        avatarsMap = singleImageMap(url = extractCommunityAvatar()),
                        coversMap = singleImageMap(url = extractCommunityCover())
                )
                else -> PageInfo(
                        nick = path.removePrefix("/").substringBefore("/"),
                        name = extractUserName(),
                        description = extractUserDescription(),
                        avatarsMap = singleImageMap(url = extractUserAvatar()),
                        coversMap = singleImageMap(url = extractUserCover())
                )
            }
        }
    }

    private suspend fun getPage(path: String): Document? {
        return client.fetchDocument(url = baseUrl.buildFullURL(path = path))
    }

    override suspend fun resolve(media: Media): Media {
        return client.fetchMediaWithOpenGraphMeta(media)
    }

    private fun JsonNode?.prepareMetaInfoMap(): Map<String, JsonNode> {
        return this
                ?.get("pre_display_requires")
                ?.map { it.findPath("__bbox") }
                ?.mapNotNull { it?.getByPath("result.data.feedback") }
                ?.map { it.getString("share_fbid").orEmpty() to it }
                ?.toMap()
                .orEmpty()
    }

    private fun Document?.extractJsonData(): JsonNode? {
        val infoJsonPrefix = "new (require(\"ServerJS\"))().handle("
        val infoJsonSuffix = ");"

        return this
                ?.getElementsByTag("script")
                ?.find { s -> s.html().startsWith(infoJsonPrefix) }
                ?.run {
                    html()
                            .removePrefix(infoJsonPrefix)
                            .removeSuffix(infoJsonSuffix)
                            .readJsonNodes()
                }
    }

    private fun Document?.extractPosts(limit: Int): List<Element> {
        return this
                ?.getElementsByClass("userContentWrapper")
                ?.take(limit)
                .orEmpty()
    }

    private fun Element.extractPostId(): String {
        return getFirstElementByAttributeValue("name", "ft_ent_identifier")
                ?.attr("value")
                .orEmpty()
    }

    private fun Element.extractPostText(): String? {
        return getFirstElementByClass("userContent")
                ?.getFirstElementByTag("p")
                ?.wholeText()
                ?.toString()
    }

    private fun Element.extractAdditionalText(): Array<String?>{
        val additionalTextElement = getFirstElementByClass("accessible_elem inlineBlock")

        return arrayOf(additionalTextElement?.parent()?.getFirstAttr("aria-label")?.toString(),
                additionalTextElement?.wholeText()?.toString())
    }

    private fun Element.extractPostPublishDateTime(): Long? {
        return getFirstElementByAttribute("data-utime")
                ?.attr("data-utime")
                ?.toLongOrNull()
    }

    private fun JsonNode.extractPostReactionCount(): Int? {
        return getInt("reaction_count.count")
    }

    private fun JsonNode.extractPostCommentsCount(): Int? {
        return getInt("display_comments_count.count")
    }

    private fun JsonNode.extractPostViewsCount(): Int? {
        return getInt("seen_by_count.count")

    }

    private fun Document.extractCommunityCover(): String? {
        return extractJsonData()
                ?.findPath("coverPhotoData")
                ?.getString("uri")
    }

    private fun Document.extractCommunityAvatar(): String? {
        return getFirstElementByAttributeValue("property", "og:image")
                ?.attr("content")
    }

    private fun Element.extractCommunityDescription(): String {
        return getFirstElementByClass("stat_elem")
                ?.getElementsByTag("span")
                ?.getOrNull(1)
                ?.wholeText()
                .orEmpty()
    }

    private fun Element.extractCommunityName(): String {
        return getFirstElementByClass("stat_elem")
                ?.getFirstElementByTag("span")
                ?.parent()
                ?.wholeText()
                .orEmpty()
    }

    private fun Element.extractUserDescription(): String? {
        return getElementsByClass("experience")
                .getOrNull(1)
                ?.allElements
                ?.lastOrNull()
                ?.wholeText()
    }

    private fun Element.extractUserName(): String? {
        return getFirstElementByAttributeValue("data-testid", "profile_name_in_profile_page")
                ?.wholeText()
    }

    private fun Element.extractUserAvatar(): String? {
        return getFirstElementByClass("profilePicThumb")
                ?.getFirstElementByTag("img")
                ?.attr("src")
    }

    private fun Element.extractUserCover(): String? {
        return getFirstElementByClass("coverPhotoImg")
                ?.attr("src")
    }

    private fun Element.extractPostMediaItems(): List<Media> {
        val videoElement = getFirstElementByTag("video")

        return when {
            videoElement != null -> listOf(Video(
                    url = getFirstElementByAttributeValueContaining("id", "feed_subtitle")
                            ?.getFirstElementByTag("a")
                            ?.attr("href")
                            ?.let { "${baseUrl}${it}" }
                            .orEmpty(),
                    aspectRatio = videoElement
                            .attr("data-original-aspect-ratio")
                            ?.toDoubleOrNull()
            ))

            else -> getFirstElementByClass("uiScaledImageContainer")
                    ?.getFirstElementByTag("img")
                    ?.run {
                        val url = attr("src")?.let {
                            when {
                                "safe_image.php" in it -> it.queryParams["url"]
                                else -> it
                            }
                        }.orEmpty()

                        listOf(Image(
                                url = url,
                                aspectRatio = attr("width").toDoubleOrNull() / attr("height").toDoubleOrNull()
                        ))
                    }
                    ?: emptyList()
        }
    }
}