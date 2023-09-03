package ru.sokomishalov.skraper.provider.linkedin

import org.junit.jupiter.api.Test
import ru.sokomishalov.skraper.model.Image
import ru.sokomishalov.skraper.model.Video
import ru.sokomishalov.skraper.provider.AbstractSkraperTest

class LinkedInSkraperTest : AbstractSkraperTest() {
    override val skraper = LinkedInSkraper(client = client)
    private val username: String = "memes"

    @Test
    fun `Check user posts`() {
        assertPosts { skraper.getUserPosts(username = username) }
    }

    @Test
    fun `Check user info`() {
        assertPageInfo { skraper.getUserInfo(username = username) }
    }

    @Test
    fun `Check media resolving`() {
        assertMediaResolved(Video("https://www.instagram.com/p/B-flad2F5o7/"))
        assertMediaResolved(Image("https://www.instagram.com/p/B-gwQJelNjs/"))
    }

    @Test
    fun `Check media downloading`() {
        assertMediaDownloaded(Video("https://www.instagram.com/p/B-flad2F5o7/"))
    }
}