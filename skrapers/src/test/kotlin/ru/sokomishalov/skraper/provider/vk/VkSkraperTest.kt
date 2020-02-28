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
package ru.sokomishalov.skraper.provider.vk

import org.junit.Test
import ru.sokomishalov.skraper.provider.SkraperTck


/**
 * @author sokomishalov
 */
class VkSkraperTest : SkraperTck() {
    override val skraper: VkSkraper = VkSkraper(client = client)
    override val path: String = "/durov"
    private val username: String = "durov"
    private val community: String = "komment"

    @Test
    fun `Check user posts`() {
        assertPosts { skraper.getUserPosts(username = username) }
    }

    @Test
    fun `Check community posts`() {
        assertPosts { skraper.getCommunityPosts(community = community) }
    }

    @Test
    fun `Check community logo`() {
        assertLogo { skraper.getCommunityLogoUrl(community = community) }
    }

    @Test
    fun `Check user logo`() {
        assertLogo { skraper.getUserLogoUrl(username = username) }
    }
}