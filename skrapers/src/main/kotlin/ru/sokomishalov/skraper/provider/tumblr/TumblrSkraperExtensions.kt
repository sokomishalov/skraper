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
package ru.sokomishalov.skraper.provider.tumblr

import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_LIMIT
import ru.sokomishalov.skraper.model.Post


/**
 * @author sokomishalov
 */

suspend fun TumblrSkraper.getUserPosts(username: String, limit: Int = DEFAULT_POSTS_LIMIT): List<Post> {
    return getUserPage(username = username).extractPosts(limit = limit)
}

suspend fun TumblrSkraper.getUserLogoUrl(username: String): String? {
    return getUserPage(username = username).extractLogo()
}
