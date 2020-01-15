@file:Suppress("ComplexRedundantLet")

package ru.sokomishalov.skraper.impls.ifunny

import org.springframework.web.util.UriComponentsBuilder
import ru.sokomishalov.memeory.core.dto.AttachmentDTO
import ru.sokomishalov.memeory.core.dto.ChannelDTO
import ru.sokomishalov.memeory.core.dto.MemeDTO
import ru.sokomishalov.memeory.core.enums.AttachmentType.IMAGE
import ru.sokomishalov.memeory.core.enums.Provider
import ru.sokomishalov.memeory.core.enums.Provider.IFUNNY
import ru.sokomishalov.skraper.ProviderService
import ru.sokomishalov.skraper.internal.consts.DELIMITER
import ru.sokomishalov.skraper.internal.consts.IFUNNY_URL
import ru.sokomishalov.skraper.internal.time.mockDate
import ru.sokomishalov.skraper.internal.jsoup.fetchDocument
import ru.sokomishalov.skraper.internal.jsoup.getSingleElementByClass
import ru.sokomishalov.skraper.internal.jsoup.getSingleElementByTag

/**
 * @author sokomishalov
 */
class IFunnyProviderService : ProviderService {

    override suspend fun fetchMemes(channel: ChannelDTO, limit: Int): List<MemeDTO> {
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

                            MemeDTO(
                                    id = "${channel.id}$DELIMITER${link.convertUrlToId()}",
                                    publishedAt = mockDate(i),
                                    attachments = listOf(AttachmentDTO(
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

    override suspend fun getLogoUrl(channel: ChannelDTO): String? {
        return fetchDocument("${IFUNNY_URL}/${channel.uri}")
                ?.getElementsByTag("meta")
                ?.find { it.attr("property") == "og:image" }
                ?.attr("content")
    }

    override val provider: Provider = IFUNNY

    private fun String.convertUrlToId(): String? {
        return UriComponentsBuilder
                .fromUriString(this)
                .replaceQuery(null)
                .build(emptyMap<String, String>())
                .toASCIIString()
                .replace("/", DELIMITER)
    }
}