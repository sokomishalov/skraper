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

import java.nio.charset.Charset
import java.time.Duration

internal const val USER_AGENT_HEADER = "User-Agent"
internal const val ACCEPT_LANGUAGE_HEADER = "Accept-Language"
internal const val DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/72.0.3626.121 Safari/537.36"
internal const val DEFAULT_ACCEPT_LANGUAGE_HEADER = "en-US"

@JvmField
internal val DEFAULT_HEADERS = mapOf(USER_AGENT_HEADER to DEFAULT_USER_AGENT, ACCEPT_LANGUAGE_HEADER to DEFAULT_ACCEPT_LANGUAGE_HEADER)

internal const val DEFAULT_POSTS_BATCH = 50

internal val DEFAULT_CONNECTION_TIMEOUT = Duration.ofSeconds(5)
internal val DEFAULT_READ_TIMEOUT = Duration.ofMinutes(1)

internal val WIN_1251_ENCODING = Charset.forName("windows-1251")