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
package ru.sokomishalov.skraper

import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_LIMIT
import ru.sokomishalov.skraper.model.ImageSize
import ru.sokomishalov.skraper.model.ImageSize.SMALL
import ru.sokomishalov.skraper.model.Post

/**
 * @author sokomishalov
 */
interface Skraper {

    /**
     * @return http client for fetching web pages, images and json from network
     */
    val client: SkraperClient get() = DefaultBlockingSkraperClient()

    /**
     * @param uri specific uri for the page
     * @param imageSize choice for specific logo size if it's possible
     * @return logo url
     */
    suspend fun getPageLogoUrl(uri: String, imageSize: ImageSize = SMALL): String?

    /**
     * @param uri specific uri for the page
     * @param limit limit for amount of posts to return
     * @param fetchAspectRatio whether to fetch or not attachment's aspect ratio if it's impossible to scrape image dimensions
     * @return list of posts
     */
    suspend fun getLatestPosts(uri: String, limit: Int = DEFAULT_POSTS_LIMIT, fetchAspectRatio: Boolean = false): List<Post>

}
