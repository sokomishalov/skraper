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
package ru.sokomishalov.skraper.client

import kotlinx.coroutines.runBlocking
import org.junit.Test
import ru.sokomishalov.skraper.*
import kotlin.math.abs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

abstract class SkraperClientTck {

    protected abstract val client: SkraperClient

    @Test
    fun `Fetch byte array assertions`() = runBlocking {
        val bytes = client.fetch("https://www.wikipedia.org/")

        assertTrue { bytes != null }
        assertTrue { bytes!!.isNotEmpty() }
    }

    @Test
    fun `Redirect to https assertion`() = runBlocking {
        val bytes = client.fetch("http://twitter.com/")

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

        assertNotNull(reposJson)
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