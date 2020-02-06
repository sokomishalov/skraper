package ru.sokomishalov.skraper.provider.tumblr

import org.jsoup.nodes.Document
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.internal.jsoup.getSingleElementByClassOrNull
import ru.sokomishalov.skraper.internal.jsoup.getSingleElementByTagOrNull
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.Post

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

        // todo

        return emptyList()
    }

    private suspend fun getPage(uri: String): Document? {
        return client.fetchDocument("https://${uri}.tumblr.com")
    }
}