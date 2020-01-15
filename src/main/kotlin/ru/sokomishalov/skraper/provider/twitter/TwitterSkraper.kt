package ru.sokomishalov.skraper.provider.twitter

import org.jsoup.nodes.Element
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.internal.dto.SkraperAttachmentDTO
import ru.sokomishalov.skraper.internal.dto.SkraperAttachmentType.IMAGE
import ru.sokomishalov.skraper.internal.dto.SkraperChannelDTO
import ru.sokomishalov.skraper.internal.dto.SkraperPostDTO
import ru.sokomishalov.skraper.internal.util.consts.DELIMITER
import ru.sokomishalov.skraper.internal.util.consts.TWITTER_URL
import ru.sokomishalov.skraper.internal.util.http.getImageAspectRatio
import ru.sokomishalov.skraper.internal.util.jsoup.fetchDocument
import ru.sokomishalov.skraper.internal.util.jsoup.getSingleElementByClass
import ru.sokomishalov.skraper.internal.util.jsoup.removeLinks
import java.util.*


/**
 * @author sokomishalov
 */
class TwitterSkraper : Skraper {

    override suspend fun fetchPosts(channel: SkraperChannelDTO, limit: Int): List<SkraperPostDTO> {
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
                    SkraperPostDTO(
                            id = "${channel.id}$DELIMITER${extractIdFromTweet(it)}",
                            caption = extractCaptionFromTweet(it),
                            publishedAt = extractPublishedAtFromTweet(it),
                            attachments = extractAttachmentsFromTweet(it)
                    )
                }
    }

    override suspend fun getChannelLogoUrl(channel: SkraperChannelDTO): String? {
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

    private suspend fun extractAttachmentsFromTweet(tweet: Element): List<SkraperAttachmentDTO> {
        return tweet
                .getElementsByClass("AdaptiveMedia-photoContainer")
                ?.map { element ->
                    element.attr("data-image-url").let {
                        SkraperAttachmentDTO(
                                url = it,
                                type = IMAGE,
                                aspectRatio = getImageAspectRatio(it)
                        )
                    }
                }
                ?: emptyList()
    }
}