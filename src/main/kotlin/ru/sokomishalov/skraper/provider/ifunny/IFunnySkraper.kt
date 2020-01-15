@file:Suppress("ComplexRedundantLet")

package ru.sokomishalov.skraper.provider.ifunny

import org.springframework.web.util.UriComponentsBuilder
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.internal.dto.SkraperAttachmentDTO
import ru.sokomishalov.skraper.internal.dto.SkraperAttachmentType.IMAGE
import ru.sokomishalov.skraper.internal.dto.SkraperChannelDTO
import ru.sokomishalov.skraper.internal.dto.SkraperPostDTO
import ru.sokomishalov.skraper.internal.util.consts.DELIMITER
import ru.sokomishalov.skraper.internal.util.consts.IFUNNY_URL
import ru.sokomishalov.skraper.internal.util.jsoup.fetchDocument
import ru.sokomishalov.skraper.internal.util.jsoup.getSingleElementByClass
import ru.sokomishalov.skraper.internal.util.jsoup.getSingleElementByTag
import ru.sokomishalov.skraper.internal.util.time.mockDate

/**
 * @author sokomishalov
 */
class IFunnySkraper : Skraper {

    override suspend fun fetchPosts(channel: SkraperChannelDTO, limit: Int): List<SkraperPostDTO> {
        val document = fetchDocument("${IFUNNY_URL}/${channel.uri}")

        val posts = document
                ?.getSingleElementByClass("feed__list")
                ?.getElementsByClass("stream__item")
                ?.take(limit)
                .orEmpty()

        return posts
                .mapIndexed { i, it ->
                    it.getSingleElementByTag("a").let { a ->
                        a.getSingleElementByTag("img").let { img ->
                            val link = a.attr("href")

                            // videos and gifs cannot be scraped :(
                            if ("video" in link || "gif" in link) return@mapIndexed null

                            SkraperPostDTO(
                                    id = "${channel.id}$DELIMITER${link.convertUrlToId()}",
                                    publishedAt = mockDate(i),
                                    attachments = listOf(SkraperAttachmentDTO(
                                            url = img.attr("data-src"),
                                            type = IMAGE,
                                            aspectRatio = it.attr("data-ratio").toDoubleOrNull()?.let { 1.div(it) }
                                    ))
                            )
                        }
                    }
                }
                .filterNotNull()
    }

    override suspend fun getChannelLogoUrl(channel: SkraperChannelDTO): String? {
        return fetchDocument("${IFUNNY_URL}/${channel.uri}")
                ?.getElementsByTag("meta")
                ?.find { it.attr("property") == "og:image" }
                ?.attr("content")
    }

    private fun String.convertUrlToId(): String? {
        return UriComponentsBuilder
                .fromUriString(this)
                .replaceQuery(null)
                .build(emptyMap<String, String>())
                .toASCIIString()
                .replace("/", DELIMITER)
    }
}