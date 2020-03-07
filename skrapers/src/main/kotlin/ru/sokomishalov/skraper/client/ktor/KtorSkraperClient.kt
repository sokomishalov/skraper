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
package ru.sokomishalov.skraper.client.ktor

import io.ktor.client.HttpClient
import io.ktor.client.request.get
import io.ktor.client.request.header
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.model.URLString

class KtorSkraperClient(
        private val client: HttpClient = DEFAULT_CLIENT
) : SkraperClient {

    override suspend fun fetch(url: URLString, headers: Map<String, String>): ByteArray? {
        return client.get(url) {
            headers.forEach { (k, v) -> header(k, v) }
        }
    }

    companion object {
        private val DEFAULT_CLIENT = HttpClient {
            followRedirects = true
        }
    }
}