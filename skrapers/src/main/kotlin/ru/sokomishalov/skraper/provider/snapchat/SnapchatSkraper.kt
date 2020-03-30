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
package ru.sokomishalov.skraper.provider.snapchat

import com.fasterxml.jackson.databind.JsonNode
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.fetchJson
import ru.sokomishalov.skraper.internal.serialization.getLong
import ru.sokomishalov.skraper.internal.serialization.getString
import ru.sokomishalov.skraper.model.*

/**
 * @author sokomishalov
 */
class SnapchatSkraper @JvmOverloads constructor(
        override val client: SkraperClient = DefaultBlockingSkraperClient,
        private val apiSearchUrl: URLString = "https://search.snapchat.com",
        override val baseUrl: URLString = "https://story.snapchat.com"
) : Skraper {

    override suspend fun getPageInfo(path: String): PageInfo? {
        val json = getUserStories(path)

        return json?.run {
            PageInfo(
                    nick = getString("userName"),
                    name = getString("storyTitle")
            )
        }
    }

    override suspend fun getPosts(path: String, limit: Int): List<Post> {
        val json = getUserStories(path)

        val postsJson = json
                ?.get("snapList")
                ?.take(limit)
                .orEmpty()

        return postsJson.map {
            with(it) {
                Post(
                        id = getString("snapId").orEmpty(),
                        text = getString("snapTitle"),
                        publishedAt = getLong("timestampInSec"),
                        media = listOf(
                                when {
                                    getString("snapUrls.overlayUrl").isNullOrBlank() -> getString("snapUrls.mediaUrl").orEmpty().toImage()
                                    else -> getString("snapUrls.mediaUrl").orEmpty().toVideo()
                                }
                        )
                )
            }
        }
    }

    private suspend fun getUserStories(path: String): JsonNode? {
        val username = path
                .removePrefix("/")
                .removePrefix("s/")

        return client.fetchJson(
                url = apiSearchUrl.buildFullURL(
                        path = "lookupStory",
                        queryParams = mapOf("id" to username)
                ),
                headers = mapOf(
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/79.0.3945.79 Safari/537.36"
                )
        )
    }
}