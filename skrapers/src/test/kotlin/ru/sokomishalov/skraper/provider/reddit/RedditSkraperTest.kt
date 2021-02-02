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
package ru.sokomishalov.skraper.provider.reddit

import org.junit.jupiter.api.Test
import ru.sokomishalov.skraper.model.Image
import ru.sokomishalov.skraper.model.Video
import ru.sokomishalov.skraper.provider.SkraperTck

/**
 * @author sokomishalov
 */
class RedditSkraperTest : SkraperTck() {
    override val skraper: RedditSkraper = RedditSkraper(client = client)
    override val path: String = "/r/memes"
    private val community: String = "r/videos"
    private val username: String = "u/ShittyMorph"


    @Test
    fun `Check community hot posts`() {
        assertPosts { skraper.getCommunityHotPosts(community = community) }
    }

    @Test
    fun `Check community new posts`() {
        assertPosts { skraper.getCommunityNewPosts(community = community) }
    }

    @Test
    fun `Check community rising posts`() {
        assertPosts { skraper.getCommunityRisingPosts(community = community) }
    }

    @Test
    fun `Check community controversial posts`() {
        assertPosts { skraper.getCommunityControversialPosts(community = community) }
    }

    @Test
    fun `Check community top posts`() {
        assertPosts { skraper.getCommunityTopPosts(community = community) }
    }

    @Test
    fun `Check user info`() {
        assertPageInfo { skraper.getUserInfo(username = username) }
    }

    @Test
    fun `Check community info`() {
        assertPageInfo { skraper.getCommunityInfo(community = community) }
    }

    @Test
    fun `Check media resolving`() {
        assertMediaResolved(Image("https://www.reddit.com/r/memes/comments/fu78mt/assuming_birds_are_real/"))
    }

    @Test
    fun `Check media downloading`() {
        assertMediaDownloaded(Video("https://www.reddit.com/r/videos/comments/geditz/frankie_macdonald_declares_2021_to_be_the_year_of/"))
    }
}