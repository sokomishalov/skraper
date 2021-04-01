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
package ru.sokomishalov.skraper.model

import java.time.Instant

/**
 * Represents a provider some user/community/channel/topic/trend post.
 * @property id provider's internal post id
 * @property text title and/or description
 * @property publishedAt publish timestamp (nullable - such data may not exist at all)
 * @property rating rating (likes, pins, etc.) count (nullable - such data may not exist at all)
 * @property commentsCount comments count (nullable - such data may not exist at all)
 * @property viewsCount views count (nullable - such data may not exist at all)
 * @property media post media items
 */
data class Post(
    val id: String,
    val text: String? = "",
    val publishedAt: Instant? = null,
    val rating: Int? = null,
    val commentsCount: Int? = null,
    val viewsCount: Int? = null,
    val media: List<Media> = emptyList()
)