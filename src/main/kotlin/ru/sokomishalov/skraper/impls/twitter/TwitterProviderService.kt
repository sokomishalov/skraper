package ru.sokomishalov.skraper.impls.twitter

import org.jsoup.nodes.Element
import ru.sokomishalov.memeory.core.dto.AttachmentDTO
import ru.sokomishalov.memeory.core.dto.ChannelDTO
import ru.sokomishalov.memeory.core.dto.MemeDTO
import ru.sokomishalov.memeory.core.enums.AttachmentType.IMAGE
import ru.sokomishalov.memeory.core.enums.Provider
import ru.sokomishalov.memeory.core.enums.Provider.TWITTER
import ru.sokomishalov.skraper.ProviderService
import ru.sokomishalov.skraper.internal.consts.DELIMITER
import ru.sokomishalov.skraper.internal.consts.TWITTER_URL
import ru.sokomishalov.skraper.internal.http.getImageAspectRatio
import ru.sokomishalov.skraper.internal.jsoup.fetchDocument
import ru.sokomishalov.skraper.internal.jsoup.getSingleElementByClass
import ru.sokomishalov.skraper.internal.jsoup.removeLinks
import java.util.*


/**
 * @author sokomishalov
 */
class TwitterProviderService : ProviderService {

    override suspend fun fetchMemes(channel: ChannelDTO, limit: Int): List<MemeDTO> {
        val webPage = fetchDocument("$TWITTER_URL/${channel.uri}")

        val posts = webPage
                ?.body()
                ?.getElementById("stream-items-id")
                ?.getElementsByClass("stream-item")
                ?.take(limit)
                .orEmpty()

        return posts
                .map {
                    it.getSingleElementByClass("tweet")
                }
                .map {
                    MemeDTO(
                            id = "${channel.id}$DELIMITER${extractIdFromTweet(it)}",
                            caption = extractCaptionFromTweet(it),
                            publishedAt = extractPublishedAtFromTweet(it),
                            attachments = extractAttachmentsFromTweet(it)
                    )
                }
    }

    override suspend fun getLogoUrl(channel: ChannelDTO): String? {
        return fetchDocument("$TWITTER_URL/${channel.uri}")
                ?.body()
                ?.getSingleElementByClass("ProfileAvatar-image")
                ?.attr("src")
    }

    override val provider: Provider = TWITTER

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

    private suspend fun extractAttachmentsFromTweet(tweet: Element): List<AttachmentDTO> {
        return tweet
                .getElementsByClass("AdaptiveMedia-photoContainer")
                ?.map { element ->
                    element.attr("data-image-url").let {
                        AttachmentDTO(
                                url = it,
                                type = IMAGE,
                                aspectRatio = getImageAspectRatio(it)
                        )
                    }
                }
                ?: emptyList()
    }
}