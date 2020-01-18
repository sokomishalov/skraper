package ru.sokomishalov.skraper

import kotlinx.coroutines.runBlocking
import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class SkraperClientTck {

    protected abstract val client: SkraperClient

    @Test
    fun `Fetch byte array assertions`() = runBlocking {
        val bytes = client.fetchBytes("https://www.wikipedia.org/")

        assertTrue { bytes != null }
        assertTrue { bytes!!.isNotEmpty() }
    }

    @Test
    fun `Redirect to https assertion`() = runBlocking {
        val bytes = client.fetchBytes("http://twitter.com/")

        assertTrue { bytes != null }
        assertTrue { bytes!!.isNotEmpty() }
    }

    @Test
    fun `Fetch document assertion`() = runBlocking {
        val document = client.fetchDocument("https://facebook.com")

        assertTrue { document != null }
        assertTrue { document!!.body().hasParent() }
    }

    @Test
    fun `Fetch json example`() = runBlocking {
        val user = "sokomishalov"
        val reposJson = client.fetchJson("https://api.github.com/users/$user/repos")

        assertTrue { reposJson.isArray }
        assertTrue { reposJson[0]["owner"]["login"].asText().toLowerCase() == user }
    }

    @Test
    fun `Fetch aspect ratio`() = runBlocking {
        val width = 200
        val height = 300
        val aspectRatio = width.toDouble() / height.toDouble()

        val fetchAspectRatio = client.fetchAspectRatio("https://picsum.photos/${width}/${height}")

        assertTrue { abs(fetchAspectRatio - aspectRatio) <= 0.01 }
    }

    @Test
    fun `Bad pages errors`() = runBlocking {
        assertNull(client.fetchBytes("https://very-badurl.badurl"))
    }
}