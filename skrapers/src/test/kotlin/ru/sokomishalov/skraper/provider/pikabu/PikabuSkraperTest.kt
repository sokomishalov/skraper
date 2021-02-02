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
package ru.sokomishalov.skraper.provider.pikabu

import org.junit.jupiter.api.Test
import ru.sokomishalov.skraper.model.Image
import ru.sokomishalov.skraper.model.Video
import ru.sokomishalov.skraper.provider.SkraperTck

class PikabuSkraperTest : SkraperTck() {
    override val skraper: PikabuSkraper = PikabuSkraper(client = client)
    override val path: String = "/@admin"
    private val username: String = "admin"
    private val community: String = "pikabu"


    @Test
    fun `Check hot posts`() {
        assertPosts { skraper.getHotPosts() }
    }

    @Test
    fun `Check best posts`() {
        assertPosts { skraper.getBestPosts() }
    }

    @Test
    fun `Check new posts`() {
        assertPosts { skraper.getNewPosts() }
    }

    @Test
    fun `Check community hot posts`() {
        assertPosts { skraper.getCommunityHotPosts(community = community) }
    }

    @Test
    fun `Check community best posts`() {
        assertPosts { skraper.getCommunityBestPosts(community = community) }
    }

    @Test
    fun `Check community new posts`() {
        assertPosts { skraper.getCommunityNewPosts(community = community) }
    }

    @Test
    fun `Check user latest posts`() {
        assertPosts { skraper.getUserLatestPosts(username = username) }
    }

    @Test
    fun `Check user best posts`() {
        assertPosts { skraper.getUserBestPosts(username = username) }
    }

    @Test
    fun `Check user own posts`() {
        assertPosts { skraper.getUserOwnPosts(username = username) }
    }

    @Test
    fun `Check user page`() {
        assertPageInfo { skraper.getUserInfo(username = username) }
    }

    @Test
    fun `Check community info`() {
        assertPageInfo { skraper.getCommunityInfo(community = community) }
    }

    @Test
    fun `Check media resolving`() {
        assertMediaResolved(Video("https://pikabu.ru/story/kogda_est_znaniya_i_umeniya_no_otsutstvuet_internet_7347966"))
        assertMediaResolved(Image("https://pikabu.ru/story/molodtsom_7348357"))
    }

    @Test
    fun `Check media downloading`() {
        assertMediaDownloaded(Video("https://pikabu.ru/story/kogda_est_znaniya_i_umeniya_no_otsutstvuet_internet_7347966"))
    }
}