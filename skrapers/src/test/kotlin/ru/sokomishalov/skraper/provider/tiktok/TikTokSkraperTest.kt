package ru.sokomishalov.skraper.provider.tiktok

import org.junit.jupiter.api.Test
import ru.sokomishalov.skraper.model.Video
import ru.sokomishalov.skraper.provider.SkraperTck

class TikTokSkraperTest : SkraperTck() {
    override val skraper: TikTokSkraper = TikTokSkraper(client = client)
    override val path: String = "/@meme"
    private val username: String = "memes"

    @Test
    fun `Check user posts`() {
        assertPosts { skraper.getUserPosts(username = username) }
    }

    @Test
    fun `Check user info`() {
        assertPageInfo{ skraper.getUserInfo(username = username) }
    }

    @Test
    fun `Check media resolving`() {
        assertMediaResolved(Video("https://www.tiktok.com/@memes/video/6912531581743762694"))
    }

    @Test
    fun `Check media downloading`() {
        assertMediaDownloaded(Video("https://www.tiktok.com/@memes/video/6912531581743762694"))
    }
}