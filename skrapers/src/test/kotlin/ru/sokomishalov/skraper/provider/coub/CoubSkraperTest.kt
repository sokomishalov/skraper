package ru.sokomishalov.skraper.provider.coub

import org.junit.jupiter.api.Test
import ru.sokomishalov.skraper.model.Video
import ru.sokomishalov.skraper.provider.SkraperTck

class CoubSkraperTest : SkraperTck() {
    override val skraper: CoubSkraper = CoubSkraper(client = client)
    override val path: String = "/haladdin"
    private val username = "haladdin"

    @Test
    fun `Check user posts`() {
        assertPosts { skraper.getUserPosts(username = username) }
    }

    @Test
    fun `Check user page info`() {
        assertPageInfo { skraper.getUserInfo(username = username) }
    }

    @Test
    fun `Check media resolving`() {
        assertMediaResolved(Video("https://coub.com/view/33nsi8"))
    }

    @Test
    fun `Check media downloading`() {
        assertMediaDownloaded(Video("https://coub.com/view/33nsi8"))
    }
}