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
@file:Suppress("unused")

package ru.sokomishalov.skraper

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import ru.sokomishalov.skraper.internal.util.consts.DEFAULT_POSTS_LIMIT
import ru.sokomishalov.skraper.internal.util.http.fetchByteArray
import ru.sokomishalov.skraper.model.Post
import ru.sokomishalov.skraper.model.ProviderChannel

/**
 * @author sokomishalov
 */

@ExperimentalCoroutinesApi
fun Skraper.getLatestPostsFlow(channel: ProviderChannel): Flow<Post> = flow { getLatestPosts(channel = channel, limit = DEFAULT_POSTS_LIMIT).asFlow().take(DEFAULT_POSTS_LIMIT) }

suspend fun Skraper.getChannelLogoByteArray(channel: ProviderChannel): ByteArray? = getChannelLogoUrl(channel)?.let { fetchByteArray(it) }


