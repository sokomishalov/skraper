package ru.sokomishalov.skraper.provider.tumblr

import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.Post

class TumblrSkraper(
        override val client: SkraperClient = DefaultBlockingSkraperClient
) : Skraper {
    override val baseUrl: String = "https://tumblr.com"
    override suspend fun getPageLogoUrl(uri: String, imageSize: ImageSize): String? = getProviderLogoUrl()
    override suspend fun getLatestPosts(uri: String, limit: Int): List<Post> = emptyList()
}