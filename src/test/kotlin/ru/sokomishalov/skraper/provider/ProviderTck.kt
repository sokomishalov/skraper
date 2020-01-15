@file:Suppress("unused")

package ru.sokomishalov.skraper.provider

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.getChannelLogoByteArray
import ru.sokomishalov.skraper.internal.dto.SkraperChannelDTO


/**
 * @author sokomishalov
 */
abstract class ProviderTck {

    protected abstract val channel: SkraperChannelDTO
    protected abstract val service: Skraper

    @Test
    fun `Check that channel memes has been fetched`() {
        val memes = runBlocking { service.fetchPosts(channel) }

        assertFalse(memes.isNullOrEmpty())
        memes.forEach {
            assertNotNull(it.id)
            assertNotNull(it.publishedAt)
            it.attachments.forEach { a ->
                assertNotNull(a.type)
                assertTrue(a.url.isNotBlank())
            }
        }
    }

    @Test
    fun `Check that channel logo has been fetched`() {
        val image = runBlocking { service.getChannelLogoByteArray(channel) } ?: ByteArray(0)

        assertNotEquals(0, image.size)
    }
}