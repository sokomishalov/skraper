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
package ru.sokomishalov.skraper.provider.ninegag

import org.junit.Test
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.ktor.KtorSkraperClient
import ru.sokomishalov.skraper.model.Image
import ru.sokomishalov.skraper.model.Video
import ru.sokomishalov.skraper.provider.SkraperTck

/**
 * @author sokomishalov
 */
class NinegagSkraperTest : SkraperTck() {
    // 9GAG added cloudflare protection. Works only with `ktor-client-cio` so far
    override val client: SkraperClient = KtorSkraperClient()
    override val skraper: NinegagSkraper = NinegagSkraper(client = client)
    override val path: String = "/meme"
    private val topic: String = "meme"
    private val tag: String = "dank-meme"

    @Test
    fun `Check hot posts`() {
        assertPosts { skraper.getHotPosts() }
    }

    @Test
    fun `Check trending posts`() {
        assertPosts { skraper.getTrendingPosts() }
    }

    @Test
    fun `Check fresh posts`() {
        assertPosts { skraper.getFreshPosts() }
    }

    @Test
    fun `Check user hot posts`() {
        assertPosts { skraper.getTopicHotPosts(topic = topic) }
    }

    @Test
    fun `Check user fresh posts`() {
        assertPosts { skraper.getTopicFreshPosts(topic = topic) }
    }

    @Test
    fun `Check tag posts`() {
        assertPosts { skraper.getTagPosts(tag = tag) }
    }

    @Test
    fun `Check topic info`() {
        assertPageInfo { skraper.getTopicInfo(topic = topic) }
    }

    @Test
    fun `Check media resolving`() {
        assertMediaResolved(Video("https://9gag.com/gag/a9RxgGZ"))
        assertMediaResolved(Image("https://9gag.com/gag/aQ1LGEq"))
    }

    @Test
    fun `Check media downloading`() {
        assertMediaDownloaded(Video("https://9gag.com/gag/a9RxgGZ"))
    }
}