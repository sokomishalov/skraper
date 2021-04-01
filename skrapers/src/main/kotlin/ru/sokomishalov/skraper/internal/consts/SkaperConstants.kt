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
package ru.sokomishalov.skraper.internal.consts

internal const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36"
internal const val USER_AGENT_HEADER = "User-Agent"
internal const val DEFAULT_POSTS_LIMIT = 50

@JvmField
internal val DEFAULT_HEADERS = mapOf(USER_AGENT_HEADER to DEFAULT_USER_AGENT)

@JvmField
internal val CRAWLER_USER_AGENTS = setOf("Googlebot", "Slurp", "Yandex", "msnbot", "bingbot")