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
package ru.sokomishalov.skraper.provider.ifunny

import org.junit.Test
import ru.sokomishalov.skraper.provider.SkraperTck


/**
 * @author sokomishalov
 */
class IFunnySkraperTest : SkraperTck() {
    override val skraper: IFunnySkraper = IFunnySkraper(client = client)
    override val path: String = "/user/memes"
    private val username: String = "memes"
    private val catalog: String = "memes"

    @Test
    fun `Check user posts`() {
        assertPosts { skraper.getUserPosts(username = username) }
    }

    @Test
    fun `Check catalog posts`() {
        assertPosts { skraper.getCatalogLatestPosts(catalog = catalog) }
    }

    @Test
    fun `Check user info`() {
        assertPageInfo { skraper.getUserInfo(username = username) }
    }
}