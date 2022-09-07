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
package ru.sokomishalov.skraper.provider.odnoklassniki

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import org.jsoup.nodes.Document
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.Skrapers
import ru.sokomishalov.skraper.client.HttpRequest
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.client.fetchDocument
import ru.sokomishalov.skraper.client.fetchOpenGraphMedia
import ru.sokomishalov.skraper.internal.iterable.emitBatch
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByAttributeValue
import ru.sokomishalov.skraper.internal.jsoup.getFirstElementByClass
import ru.sokomishalov.skraper.internal.jsoup.getMetaPropertyMap
import ru.sokomishalov.skraper.internal.net.host
import ru.sokomishalov.skraper.model.*

open class OdnoklassnikiSkraper @JvmOverloads constructor(
    override val client: SkraperClient = Skrapers.client
) : Skraper {

    override fun getPosts(path: String): Flow<Post> = flow {
        val document = getPage(path = path)

        val rawPosts = document
            ?.getElementsByAttribute("data-feed-id")
            ?.toList()
            .orEmpty()

        emitBatch(rawPosts) {
            Post(
                id = attr("data-feed-id").orEmpty(),
                text = getFirstElementByClass("media-text_cnt")?.wholeText(),
                statistics = PostStatistics(
                    likes = getFirstElementByAttributeValue("data-widget-item-type", "like")?.getFirstElementByClass("widget_count")?.html()?.trim()?.toIntOrNull(),
                    comments = getFirstElementByAttributeValue("data-widget-item-type", "comment")?.getFirstElementByClass("widget_count")?.html()?.trim()?.toIntOrNull(),
                    reposts = getFirstElementByAttributeValue("data-widget-item-type", "reshare")?.getFirstElementByClass("widget_count")?.html()?.trim()?.toIntOrNull(),
                ),
                media = getElementsByClass("vid-card").mapNotNull { it.getFirstElementByClass("vid-card_cnt_w")?.id()?.substringAfterLast("_")?.takeWhile { it.isDigit() }?.let { "${BASE_URL}/video/${it}" }?.toVideo() }
                        + getElementsByAttribute("data-mp4src").mapNotNull { it.attr("data-mp4src").let { "https:${it}" }.toVideo() }
                        + getElementsByClass("collage_img").mapNotNull { it.attr("src").let { "https:${it}" }.toImage() }
            )
        }
    }

    override suspend fun getPageInfo(path: String): PageInfo? {
        val page = getPage(path = path)
        val properties = page?.getMetaPropertyMap()

        return properties?.let {
            PageInfo(
                nick = it["og:url"]?.substringAfterLast("/"),
                name = it["og:title"].orEmpty().substringBefore(" |"),
                description = it["og:description"].orEmpty(),
                avatar = it["og:image"]?.toImage(),
            )
        }
    }

    override fun supports(url: String): Boolean {
        return "ok.ru" in url.host
    }

    override suspend fun resolve(media: Media): Media {
        return client.fetchOpenGraphMedia(media)
    }

    private suspend fun getPage(path: String): Document? {
        return client.fetchDocument(
            request = HttpRequest(url = BASE_URL.buildFullURL(path = path)),
        )
    }

    companion object {
        const val BASE_URL: String = "https://ok.ru"
    }
}