package ru.sokomishalov.skraper.provider.flickr

import com.fasterxml.jackson.databind.JsonNode
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_ASPECT_RATIO
import ru.sokomishalov.skraper.internal.jsoup.getSingleElementByClass
import ru.sokomishalov.skraper.internal.jsoup.getStyle
import ru.sokomishalov.skraper.internal.serialization.aReadJsonNodes
import ru.sokomishalov.skraper.internal.serialization.getByKeyContaining
import ru.sokomishalov.skraper.internal.serialization.getFirstByPath
import ru.sokomishalov.skraper.internal.string.unescapeHtml
import ru.sokomishalov.skraper.internal.string.unescapeUrl
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.ImageSize.*
import ru.sokomishalov.skraper.model.Post


/**
 * @author sokomishalov
 */
class FlickrSkraper(
        override val client: SkraperClient = DefaultBlockingSkraperClient,
        override val baseUrl: String = "https://flickr.com"
) : Skraper {

    override suspend fun getPosts(path: String, limit: Int): List<Post> {
        val page = getPage(path = path)

        val domPosts = page
                ?.getElementsByClass("photo-list-photo-view")
                ?.take(limit)
                .orEmpty()

        val jsonPosts = page
                .parseModelJson()
                ?.getByKeyContaining("photo")
                ?.get("_data")
                ?.map { it["id"].asText().orEmpty() to it }
                ?.toMap()
                .orEmpty()

        return when {
            domPosts.isNotEmpty() -> domPosts.map { domPost ->
                val url = domPost.getBackgroundImage().orEmpty()
                val id = url.substringAfterLast("/").substringBefore("_")

                val jsonPost = jsonPosts[id]

                Post(
                        id = id,
                        text = jsonPost?.extractText(),
                        publishedAt = jsonPost?.extractPublishDate(),
                        commentsCount = jsonPost?.extractCommentsCount(),
                        rating = jsonPost?.extractRating(),
                        attachments = listOf(Attachment(
                                type = IMAGE,
                                url = url,
                                aspectRatio = domPost.extractAspectRatio()
                        ))
                )
            }
            jsonPosts.isNotEmpty() -> jsonPosts.map {
                Post(
                        id = it.key,
                        text = it.value?.extractText(),
                        publishedAt = it.value?.extractPublishDate(),
                        commentsCount = it.value?.extractCommentsCount(),
                        rating = it.value?.extractRating(),
                        attachments = listOf(Attachment(
                                type = IMAGE,
                                url = it.value.extractAttachmentUrl(),
                                aspectRatio = it.value.extractAspectRatio()
                        ))
                )
            }
            else -> emptyList()
        }
    }

    override suspend fun getLogoUrl(path: String, imageSize: ImageSize): String? {
        val page = getPage(path = path)

        val photoUrls = page
                .parseModelJson()
                ?.get("owner")
                ?.get("buddyicon")

        return when {
            photoUrls?.isEmpty?.not() ?: false -> {
                val key = when (imageSize) {
                    SMALL -> "small"
                    MEDIUM -> "medium"
                    LARGE -> "large"
                }

                photoUrls?.get(key)?.asText().orEmpty().let { "https:${it}" }
            }
            else -> page
                    ?.getSingleElementByClass("avatar")
                    .getBackgroundImage()
        }
    }

    private suspend fun getPage(path: String): Document? = client.fetchDocument("$baseUrl$path")

    private suspend fun Document?.parseModelJson(): JsonNode? {
        val fullJson = this
                ?.getSingleElementByClass("modelExport")
                ?.html()
                ?.substringAfter("Y.ClientApp.init(")
                ?.substringBefore(".then(function()")
                ?.substringBeforeLast(")")
                ?.replace("auth: auth,", "")
                ?.replace("reqId: reqId,", "")

        return runCatching {
            val json = fullJson
                    ?.aReadJsonNodes()
                    ?.get("modelExport")
                    ?.get("main")
                    ?.getByKeyContaining("models")

            json?.firstOrNull()
        }.getOrNull()
    }

    private fun Element?.getBackgroundImage(): String? {
        return this
                ?.getStyle("background-image")
                ?.trim()
                ?.removeSurrounding("url(", ")")
                ?.let { "https:$it" }
    }

    private fun Element?.extractAspectRatio(): Double {
        return run {
            val width = getDimension("width")
            val height = getDimension("height")

            when {
                width != null && height != null -> width / height
                else -> DEFAULT_POSTS_ASPECT_RATIO
            }
        }
    }

    private fun Element?.getDimension(name: String): Double? {
        return this
                ?.getStyle(name)
                ?.trim()
                ?.removeSuffix("px")
                ?.toDoubleOrNull()
    }

    private fun JsonNode.extractText(): String {
        val title = get("title").unescapeNode()
        val description = get("description").unescapeNode()

        return "${title}\n\n${description}"
    }

    private fun JsonNode.extractPublishDate(): Long? {
        return getFirstByPath("stats.datePosted")
                ?.asLong()
                ?.times(1000)
    }

    private fun JsonNode.extractRating(): Int? {
        return getFirstByPath("engagement.commentCount", "commentCount")
                ?.asInt()
    }

    private fun JsonNode.extractCommentsCount(): Int? {
        return getFirstByPath("engagement.faveCount", "faveCount")
                ?.asInt()
    }

    private fun JsonNode.extractAttachmentUrl(): String {
        return getFirstByPath("sizes.l", "sizes.m", "sizes.s")
                ?.get("url")
                ?.asText()
                ?.let { "https:${it}" }
                .orEmpty()
    }

    private fun JsonNode.extractAspectRatio(): Double {
        return getFirstByPath("sizes.l", "sizes.m", "sizes.s")
                .run {
                    val width = this?.get("width")?.asDouble()
                    val height = this?.get("height")?.asDouble()

                    when {
                        width != null && height != null -> width / height
                        else -> DEFAULT_POSTS_ASPECT_RATIO
                    }

                }
    }

    private fun JsonNode?.unescapeNode(): String {
        return runCatching {
            this?.asText()?.unescapeUrl()?.unescapeHtml().orEmpty()
        }.getOrElse {
            this?.asText().orEmpty()
        }
    }
}
