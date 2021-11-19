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
package ru.sokomishalov.skraper.provider.imgur

import org.junit.jupiter.api.Test
import ru.sokomishalov.skraper.model.Image
import ru.sokomishalov.skraper.model.Video
import ru.sokomishalov.skraper.provider.SkraperTck


/**
 * @author sokomishalov
 */
class ImgurSkraperTest : SkraperTck() {
    override val skraper: ImgurSkraper = ImgurSkraper(client = client)
    override val path: String = "/user/ImmortalChild"
    private val username: String = "ImmortalChild"
    private val tag: String = "youtube"

    @Test
    fun `Check user posts`() {
        assertPosts { skraper.getUserPosts(username = username) }
    }

    @Test
    fun `Check tag posts`() {
        assertPosts { skraper.getTagPosts(tag = tag) }
    }

    @Test
    fun `Check user info`() {
        assertPageInfo { skraper.getUserInfo(username = username) }
    }

    @Test
    fun `Check media resolving`() {
        assertMediaResolved(Image("https://imgur.com/gallery/ePaFE9j"))
    }

    @Test
    fun `Check media downloading`() {
        assertMediaDownloaded(Video("https://imgur.com/gallery/ePaFE9j"))
    }
}