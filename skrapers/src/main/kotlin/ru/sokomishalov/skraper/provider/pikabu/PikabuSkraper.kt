package ru.sokomishalov.skraper.provider.pikabu

import org.jsoup.nodes.Document
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchDocument
import ru.sokomishalov.skraper.internal.url.uriCleanUp
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.Post

class PikabuSkraper(
        override val client: SkraperClient = DefaultBlockingSkraperClient
) : Skraper {

    override val baseUrl: String = "https://pikabu.ru"

    override suspend fun getLatestPosts(uri: String, limit: Int): List<Post> {
        val document = getUserPage(uri)
        return emptyList()
    }

    override suspend fun getPageLogoUrl(uri: String, imageSize: ImageSize): String? {
        val document = getUserPage(uri)
        return null
    }

    private suspend fun getUserPage(uri: String): Document? {
        return client.fetchDocument("${baseUrl}${uri.uriCleanUp()}")
    }
}