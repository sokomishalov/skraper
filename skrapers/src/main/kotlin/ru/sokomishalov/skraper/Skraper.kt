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
package ru.sokomishalov.skraper

import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.internal.consts.DEFAULT_LOGO_SIZE
import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_LIMIT
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.Post

/**
 * @author sokomishalov
 */
interface Skraper {

    /**
     * @return provider base url
     */
    val baseUrl: String

    /**
     * @return http client for fetching web pages, images and json from network
     */
    val client: SkraperClient get() = DefaultBlockingSkraperClient

    /**
     * @param path page specific url path (should start with "/")
     * @param limit for an amount of posts to return
     * @return list of posts
     */
    suspend fun getPosts(path: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post>

    /**
     * @param path page specific url path (should start with "/")
     * @param imageSize choice for specific logo size if it's possible
     * @return page logo url
     */
    suspend fun getLogoUrl(path: String, imageSize: ImageSize = DEFAULT_LOGO_SIZE): String?

    /**
     * @param imageSize choice for specific logo size if it's possible
     * @return provider logo url
     */
    suspend fun getProviderLogoUrl(imageSize: ImageSize = DEFAULT_LOGO_SIZE): String = "${baseUrl}/favicon.ico"
}
