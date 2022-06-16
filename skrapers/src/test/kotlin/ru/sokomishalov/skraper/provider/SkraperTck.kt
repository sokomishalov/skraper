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
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
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
import ru.sokomishalov.skraper.model.Comment
import ru.sokomishalov.skraper.model.Media
import ru.sokomishalov.skraper.model.PageInfo
import ru.sokomishalov.skraper.model.Post
import java.nio.file.Files
import java.time.Duration
import java.time.LocalTime
import java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
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
            registerModule(SimpleModule().apply {
                addSerializer(object : JsonSerializer<Duration>() {
                    override fun handledType(): Class<Duration> = Duration::class.java
                    override fun serialize(value: Duration, gen: JsonGenerator, serializers: SerializerProvider) = gen.writeString(ISO_LOCAL_TIME.format(LocalTime.of(0, 0) + value))
                })
            })
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
        val posts = log { skraper.action().take(50).toList() }

        assertNotNull(posts)
        assertTrue("Posts are empty") { posts.isNotEmpty() }
        posts.forEach {
            assertTrue("Post id cannot be blank") { it.id.isNotBlank() }
            it.media.forEach { a ->
                assertTrue("Media URL cannot be blank") { a.url.isNotBlank() }
            }
        }
    }

    protected fun assertComments(action: Skraper.() -> Flow<Comment>) = runBlocking {
        val comments = log { skraper.action().take(50).toList() }

        assertNotNull(comments)
        assertTrue("Comments are empty") { comments.isNotEmpty() }
        comments.forEach {
            assertTrue("Comment author cannot be null") { it.author != null }
            assertTrue(("Comment text cannot be null")) { it.comment.isNotEmpty() }
        }
    }

    protected fun assertPageInfo(action: suspend Skraper.() -> PageInfo?) = runBlocking {
        val pageInfo = log { action() }

        assertNotNull(pageInfo, "Page info cannot be null")
        assertFalse("Page nick cannot be blank") { pageInfo.nick.isNullOrBlank() }
        assertFalse("Page avatar cannot be blank") { pageInfo.avatar?.url.isNullOrBlank() }
    }

    protected fun assertMediaResolved(media: Media) = runBlocking {
        val canResolve = skraper.supports(media.url)
        assertTrue { canResolve }

        val resolved = log { resolve(media) }

        assertNotNull(resolved, "Resolved media cannot be null")
        assertNotEquals(media.url, resolved.url, "Resolved media url cannot be equal to the original media url")
    }

    protected fun assertMediaDownloaded(media: Media) = runBlocking {
        val tmpDir = Files.createTempDirectory("skraper").toFile()
        val downloaded = runCatching {
            log {
                Skrapers.download(
                    media = media,
                    destDir = tmpDir,
                    filename = UUID.randomUUID().toString()
                )
            }
        }.getOrNull()

        assertNotNull(downloaded)
        assertTrue { downloaded.exists() }
        assertTrue { downloaded.length() > 0 }
    }

    protected suspend fun <T> log(action: suspend Skraper.() -> T): T? {
        return runCatching { skraper.action() }
            .onSuccess { log.info(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(it)) }
            .onFailure { throw AssertionError("Exception occured", it) }
            .getOrNull()
    }
}