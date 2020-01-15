@file:Suppress("unused")

package ru.sokomishalov.skraper.impls

import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import ru.sokomishalov.memeory.core.dto.ChannelDTO
import ru.sokomishalov.memeory.core.enums.AttachmentType.NONE
import ru.sokomishalov.skraper.ProviderService
import ru.sokomishalov.skraper.internal.http.fetchByteArray


/**
 * @author sokomishalov
 */
abstract class AbstractProviderIntegrationTest {


    protected abstract val channel: ChannelDTO
    protected abstract val service: ProviderService

    @Test
    fun `Check that channel memes has been fetched`() {
        val memes = runBlocking { service.fetchMemes(channel) }

        assertFalse(memes.isNullOrEmpty())
        memes.forEach {
            assertNotNull(it.id)
            assertNotNull(it.publishedAt)
            it.attachments.forEach { a ->
                assertTrue(a.type != NONE)
                assertFalse(a.url.isNullOrBlank())
            }
        }
    }

    @Test
    fun `Check that channel logo has been fetched`() {
        val image = runBlocking { fetchByteArray(service.getLogoUrl(channel).orEmpty()) } ?: ByteArray(0)

        assertNotEquals(0, image.size)
    }
}