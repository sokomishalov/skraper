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
package ru.sokomishalov.skraper.model

/**
 * Represents a provider post.
 * @property id provider's internal id
 * @property text concatenated text
 * @property publishedAt publish timestamp in nanos *(nullable - such data may not exist on the provider's page)
 * @property rating rating (likes) count *(nullable - such data may not exist on the provider's page)
 * @property commentsCount comments count *(nullable - such data may not exist on the provider's page)
 * @property attachments images or videos
 */
data class Post(
        val id: String,
        val text: String? = "",
        val publishedAt: Long? = null,
        val rating: Int? = null,
        val commentsCount: Int? = null,
        val attachments: List<Attachment> = emptyList()
)
