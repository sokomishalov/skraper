package ru.sokomishalov.skraper.provider.pikabu

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_ASPECT_RATIO
import ru.sokomishalov.skraper.internal.url.uriCleanUp
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.AttachmentType.VIDEO
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.Post
import java.nio.charset.Charset

class PikabuSkraper(
        override val client: SkraperClient = DefaultBlockingSkraperClient
) : Skraper {

    override val baseUrl: String = "https://pikabu.ru"

    override suspend fun getLatestPosts(uri: String, limit: Int): List<Post> {
        val document = getUserPage(uri)

        val stories = document
                ?.getElementsByClass("story__main")
                ?.take(limit)
                .orEmpty()

        return stories.map {
            val storyBlocks = it.getElementsByClass("story-block")

            val title = it.parseTitle()
            val text = storyBlocks.parseText()

            val caption = when {
                text.isBlank() -> title
                else -> "${title}\n\n${text}"
            }

            Post(
                    id = it.parseId(),
                    caption = String(caption.toByteArray(Charset.forName("windows-1251"))),
                    attachments = storyBlocks.parseMediaAttachments()
            )
        }
    }

    override suspend fun getPageLogoUrl(uri: String, imageSize: ImageSize): String? {
        val document = getUserPage(uri)

        return document
                ?.getElementsByAttributeValue("property", "og:image")
                ?.firstOrNull()
                ?.attr("content")
    }

    private suspend fun getUserPage(uri: String): Document? {
        return client.fetchDocument("${baseUrl}/${uri.uriCleanUp()}")
    }

    private fun Element.parseId(): String {
        return getElementsByClass("story__title-link")
                .firstOrNull()
                ?.attr("href")
                ?.substringAfter("${baseUrl}/story/")
                .orEmpty()
    }

    private fun Element.parseTitle(): String {
        return getElementsByClass("story__title-link")
                .firstOrNull()
                ?.wholeText()
                .orEmpty()
    }

    private fun Elements.parseMediaAttachments(): List<Attachment> {
        return mapNotNull { b ->
            when {
                "story-block_type_image" in b.classNames() -> {
                    Attachment(
                            type = IMAGE,
                            url = b
                                    .getElementsByTag("img")
                                    .firstOrNull()
                                    ?.attr("data-src")
                                    .orEmpty(),
                            aspectRatio = b
                                    .getElementsByTag("rect")
                                    .firstOrNull()
                                    .run {
                                        val width = this?.attr("width")?.toDoubleOrNull()
                                        val height = this?.attr("height")?.toDoubleOrNull()

                                        when {
                                            width != null && height != null -> width / height
                                            else -> DEFAULT_POSTS_ASPECT_RATIO
                                        }
                                    }
                    )
                }
                "story-block_type_video" in b.classNames() -> b
                        .getElementsByAttributeValueContaining("data-type", "video")
                        .firstOrNull()
                        .run {
                            Attachment(
                                    type = VIDEO,
                                    url = this
                                            ?.attr("data-source")
                                            .orEmpty(),
                                    aspectRatio = this
                                            ?.attr("data-ratio")
                                            ?.toDoubleOrNull()
                                            ?: DEFAULT_POSTS_ASPECT_RATIO
                            )
                        }

                else -> null
            }
        }
    }

    private fun Elements.parseText(): String {
        return filter { b -> "story-block_type_text" in b.classNames() }
                .joinToString("\n") { b -> b.wholeText() }
    }
}


