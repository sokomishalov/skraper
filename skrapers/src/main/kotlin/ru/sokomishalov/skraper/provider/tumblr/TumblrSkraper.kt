package ru.sokomishalov.skraper.provider.tumblr

import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_ASPECT_RATIO
import ru.sokomishalov.skraper.internal.jsoup.getSingleElementByClassOrNull
import ru.sokomishalov.skraper.internal.jsoup.getSingleElementByTagOrNull
import ru.sokomishalov.skraper.model.Attachment
import ru.sokomishalov.skraper.model.AttachmentType.IMAGE
import ru.sokomishalov.skraper.model.AttachmentType.VIDEO
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.Post
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC
import java.time.format.DateTimeFormatter
import java.util.Locale.ENGLISH

class TumblrSkraper(
        override val client: SkraperClient = DefaultBlockingSkraperClient
) : Skraper {

    override val baseUrl: String = "https://tumblr.com"

    override suspend fun getPageLogoUrl(uri: String, imageSize: ImageSize): String? {
        val document = getPage(uri)

        return document
                ?.getSingleElementByClassOrNull("user-avatar")
                ?.getSingleElementByTagOrNull("img")
                ?.attr("src")
    }

    override suspend fun getLatestPosts(uri: String, limit: Int): List<Post> {
        val document = getPage(uri)

        val articles = document
                ?.getElementsByTag("article")
                ?.take(limit)
                .orEmpty()

        return articles.map { a ->
            Post(
                    id = a.extractId(),
                    text = a.extractText(),
                    publishedAt = a.extractPublishedDate(),
                    rating = a.extractNotes(),
                    commentsCount = a.extractNotes(),
                    attachments = a.extractAttachments()
            )
        }
    }

    private suspend fun getPage(uri: String): Document? {
        return client.fetchDocument("https://${uri}.tumblr.com")
    }

    private fun Element.extractId(): String {
        return attr("data-post-id")
                .ifBlank { attr("id") }
    }

    private fun Element.extractText(): String? {
        return getElementsByTag("figcaption")
                .joinToString("\n") { it.wholeText().orEmpty() }
                .substringAfter(":")
    }

    private fun Element.extractPublishedDate(): Long? {
        val postDate = getSingleElementByClassOrNull("post-date")
        val timePosted = getSingleElementByClassOrNull("time-posted")

        return when {

            postDate != null -> postDate
                    .wholeText()
                    .let { runCatching { LocalDate.parse(it, DATE_FORMATTER) }.getOrNull() }
                    ?.atStartOfDay()
                    ?.toEpochSecond(UTC)
                    ?.times(1000)

            timePosted != null -> timePosted
                    .attr("title")
                    .replace("am", "AM")
                    .replace("pm", "PM")
                    .let { runCatching { LocalDateTime.parse(it, DATE_TIME_FORMATTER) }.getOrNull() }
                    ?.toEpochSecond(UTC)
                    ?.times(1000)

            else -> null
        }
    }

    private fun Element.extractNotes(): Int? {
        val notesNode = getSingleElementByClassOrNull("post-notes")
                ?: getSingleElementByClassOrNull("note-count")

        return notesNode
                ?.wholeText()
                ?.split(" ")
                ?.firstOrNull()
                ?.replace(",", "")
                ?.replace(".", "")
                ?.toIntOrNull()
                ?: 0
    }

    private fun Element.extractAttachments(): List<Attachment> {
        return getElementsByTag("figure").mapNotNull { f ->
            val video = f.getSingleElementByTagOrNull("video")
            val img = f.getSingleElementByTagOrNull("img")

            Attachment(
                    type = when {
                        video != null -> VIDEO
                        img != null -> IMAGE
                        else -> return@mapNotNull null
                    },
                    url = when {
                        video != null -> video.getSingleElementByTagOrNull("source")?.attr("src").orEmpty()
                        else -> img?.attr("src").orEmpty()
                    },
                    aspectRatio = f.run {
                        val width = attr("data-orig-width")?.toDoubleOrNull()
                        val height = attr("data-orig-height")?.toDoubleOrNull()
                        when {
                            width != null && height != null -> width / height
                            else -> DEFAULT_POSTS_ASPECT_RATIO
                        }
                    }
            )
        }
    }

    companion object {
        private val DATE_FORMATTER = DateTimeFormatter.ofPattern("MMM d'th,' yyyy").withLocale(ENGLISH)
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("h:mm a, EEEE, MMMM d, yyyy").withLocale(ENGLISH)
    }
}
