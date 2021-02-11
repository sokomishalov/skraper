/**
 * Copyright (c) 2019-present Mikhael Sokolov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
@file:Suppress("BlockingMethodInNonBlockingContext")

package ru.sokomishalov.skraper.client

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import ru.sokomishalov.skraper.client.HttpMethodType.POST
import ru.sokomishalov.skraper.internal.serialization.getString
import java.nio.file.Files
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.text.Charsets.UTF_8

abstract class SkraperClientTck {

    protected abstract val client: SkraperClient

    @Test
    fun `Fetch byte array`() = runBlocking {
        val resp = client.request(HttpRequest("https://www.wikipedia.org/"))

        assertNotNull(resp)
        assertEquals(200, resp.status)
        assertTrue { resp.headers.isNotEmpty() }
        assertNotNull(resp.body)
        assertTrue { resp.body!!.isNotEmpty() }
    }

    @Test
    fun `Redirect to https`() = runBlocking {
        val resp = client.request(HttpRequest("http://twitter.com/"))

        assertNotNull(resp)
        assertEquals(200, resp.status)
        assertNotNull(resp.body)
        assertTrue { resp.body!!.isNotEmpty() }
    }

    @Test
    fun `Fetch document`() = runBlocking {
        val document = client.fetchDocument(HttpRequest("https://facebook.com"))

        assertNotNull(document)
        assertTrue { document.body().hasParent() }
    }

    @Test
    fun `Fetch complex json`() = runBlocking {
        val echoJson = client.fetchJson(HttpRequest(
            url = "https://postman-echo.com/post",
            method = POST,
            headers = mapOf(
                "foo" to "bar",
                "Content-Type" to "application/json"
            ),
            body = """{"bar": "foo"}""".toByteArray(UTF_8)
        ))

        assertNotNull(echoJson)
        assertEquals("bar", echoJson.getString("headers.foo"))
        assertEquals("foo", echoJson.getString("data.bar"))
    }

    @Test
    fun `Bad url`() = runBlocking {
        assertNull(client.fetchBytes(HttpRequest("https://very-badurl.badurl")))
    }

    @Test
    fun `File download`() = runBlocking {
        val tempFile = Files.createTempFile("test-pfx", ".zip").toFile().apply { deleteOnExit() }

        assertTrue { tempFile.exists() }
        assertEquals(0L, tempFile.length())

        client.download(HttpRequest("http://speedtest.tele2.net/1MB.zip"), tempFile)

        assertTrue { tempFile.exists() }
        assertEquals(1L * 1024 * 1024, tempFile.length())
    }
}