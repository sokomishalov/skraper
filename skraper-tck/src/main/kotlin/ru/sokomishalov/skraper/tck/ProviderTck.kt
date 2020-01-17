/**
 * Copyright 2019-2020 the original author or authors.
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
@file:Suppress("unused", "FunctionName")

package ru.sokomishalov.skraper.tck

import com.fasterxml.jackson.databind.json.JsonMapper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.getPageLogoByteArray


/**
 * @author sokomishalov
 */
abstract class ProviderTck {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(ProviderTck::class.java)
    }

    protected abstract val service: Skraper
    protected abstract val uri: String

    @Test
    fun `Check that posts has been fetched`() = runBlocking {
        val posts = service.getLatestPosts(uri)

        log.info(JsonMapper().writerWithDefaultPrettyPrinter().writeValueAsString(posts))

        assertTrue(posts.isNotEmpty())
        posts.forEach {
            assertNotNull(it.id)
            it.attachments.forEach { a ->
                assertNotNull(a.type)
                assertTrue(a.url.isNotBlank())
                assertTrue(a.aspectRatio > 0.0001)
            }
        }
    }

    @Test
    fun `Check that channel logo has been fetched`() = runBlocking {
        val image = service.getPageLogoByteArray(uri) ?: ByteArray(0)

        assertNotEquals(0, image.size)
    }
}