/*
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
@file:Suppress("MemberVisibilityCanBePrivate", "BlockingMethodInNonBlockingContext")

package ru.sokomishalov.skraper.provider

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.databind.json.JsonMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.Skrapers
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.client.ktor.KtorSkraperClient
import ru.sokomishalov.skraper.model.Media
import ru.sokomishalov.skraper.model.PageInfo
import ru.sokomishalov.skraper.model.Post
import java.nio.file.Files
import java.util.*
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue


/**
 * @author sokomishalov
 */
abstract class SkraperTck {

    companion object {
        @JvmStatic
        private val log: Logger = LoggerFactory.getLogger(SkraperTck::class.java)

        @JvmStatic
        private val mapper: ObjectMapper = JsonMapper().apply {
            findAndRegisterModules()
            disable(WRITE_DATES_AS_TIMESTAMPS)
            setSerializationInclusion(NON_NULL)
        }
    }

    protected abstract val skraper: Skraper
    protected abstract val path: String

    protected open val client: SkraperClient = KtorSkraperClient()

    @BeforeEach
    internal fun setUp() {
        Skrapers.client = client
    }

    @Test
    open fun `Check posts`() {
        assertPosts { getPosts(path = path) }
    }

    @Test
    open fun `Check page info`() {
        assertPageInfo { getPageInfo(path = path) }
    }

    protected fun assertPosts(action: Skraper.() -> Flow<Post>) = runBlocking {
        val posts = logAction { skraper.action().take(50).toList() }

        assertTrue { posts.isNotEmpty() }
        posts.forEach {
            assertNotNull(it.id)
            it.media.forEach { a ->
                assertTrue(a.url.isNotBlank())
            }
        }
    }

    protected fun assertPageInfo(action: suspend Skraper.() -> PageInfo?) = runBlocking {
        val pageInfo = logAction { skraper.action() }

        assertNotNull(pageInfo)
        assertNotNull(pageInfo.nick)
        assertFalse { pageInfo.avatar?.url.isNullOrBlank() }
    }

    protected fun assertMediaResolved(media: Media) = runBlocking {
        val canResolve = skraper.supports(media)
        assertTrue { canResolve }

        val resolved = logAction { skraper.resolve(media) }

        assertNotNull(resolved)
        assertNotNull(resolved.url)
        assertNotEquals(media.url, resolved.url)
    }

    protected fun assertMediaDownloaded(media: Media) = runBlocking {
        val tmpDir = Files.createTempDirectory("skraper").toFile()
        val downloaded = runCatching { logAction {
            Skrapers.download(
                media = media,
                destDir = tmpDir,
                filename = UUID.randomUUID().toString()
            )
        } }.getOrNull()

        assertNotNull(downloaded)
        assertTrue { downloaded.exists() }
        assertTrue { downloaded.length() > 0 }
    }

    protected suspend fun <T> logAction(action: suspend Skraper.() -> T): T {
        return skraper.action().also {
            log.info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(it))
        }
    }
}