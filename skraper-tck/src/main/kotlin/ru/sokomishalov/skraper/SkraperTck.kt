@file:Suppress("FunctionName")

package ru.sokomishalov.skraper

/**
 * @author sokomishalov
 */
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import ru.sokomishalov.skraper.model.SkraperChannel

/**
 * @author sokomishalov
 */
abstract class SkraperTck {

    protected abstract val channel: SkraperChannel
    protected abstract val service: Skraper

    @Test
    fun `Check that posts has been fetched`() {
        val posts = runBlocking { service.getLatestPosts(channel) }

        assertFalse(posts.isNullOrEmpty())
        posts.forEach {
            assertNotNull(it.id)
            assertNotNull(it.publishedAt)
            it.attachments.forEach { a ->
                assertNotNull(a.type)
                assertFalse(a.url.isBlank())
            }
        }
    }

    @Test
    fun `Check that channel logo has been fetched`() {
        val image = runBlocking { service.getLogoByteArray(channel) } ?: ByteArray(0)

        assertNotEquals(0, image.size)
    }
}