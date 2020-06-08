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
import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_LIMIT
import ru.sokomishalov.skraper.internal.net.host
import ru.sokomishalov.skraper.model.*

/**
 * Interface for the minimum provider functionality
 */
interface Skraper {

    /**
     * added for some static extensions
     */
    companion object

    /**
     * @return provider base url
     */
    val baseUrl: URLString

    /**
     * @return http client
     */
    val client: SkraperClient get() = DefaultBlockingSkraperClient

    /**
     * @param url potential provider relative url
     * @return true if such skraper supports this url
     */
    suspend fun supports(url: URLString): Boolean = url.host.removePrefix("www.") in baseUrl.host

    /**
     * @return provider info
     */
    suspend fun getProviderInfo(): ProviderInfo? = ProviderInfo(
            name = name,
            logoMap = singleImageMap(url = baseUrl.buildFullURL(path = "/favicon.ico"))
    )

    /**
     * @param path page specific url path (should start with "/")
     * @return page info
     */
    suspend fun getPageInfo(path: String): PageInfo?

    /**
     * @param path page specific url path (should start with "/")
     * @param limit for an amount of posts to return
     * @return list of posts
     */
    suspend fun getPosts(path: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post>

    /**
     * @param media with provider relative url
     * @return media with direct url
     */
    suspend fun resolve(media: Media): Media
}
