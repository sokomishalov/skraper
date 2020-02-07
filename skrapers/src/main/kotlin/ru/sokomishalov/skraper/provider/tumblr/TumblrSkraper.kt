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
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.Post
import java.time.LocalDate
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
                    publishTimestamp = a.extractPublishedDate(),
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
    }

    private fun Element.extractPublishedDate(): Long? {
        return getSingleElementByClassOrNull("post-date")
                ?.wholeText()
                ?.let { runCatching { LocalDate.parse(it, DATE_FORMATTER) }.getOrNull() }
                ?.atStartOfDay()
                ?.toEpochSecond(UTC)
                ?.times(1000)
    }

    private fun Element.extractNotes(): Int? {
        return getSingleElementByClassOrNull("post-notes")
                ?.wholeText()
                ?.split(" ")
                ?.firstOrNull()
                ?.toIntOrNull()
    }

    private fun Element.extractAttachments(): List<Attachment> {
        return getElementsByTag("figure").map { f ->
            val img = f.getSingleElementByTagOrNull("img")
            Attachment(
                    type = IMAGE,
                    url = img?.attr("src").orEmpty(),
                    aspectRatio = img.let {
                        val width = it?.attr("width")?.toDoubleOrNull()
                        val height = it?.attr("height")?.toDoubleOrNull()
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
    }
}
