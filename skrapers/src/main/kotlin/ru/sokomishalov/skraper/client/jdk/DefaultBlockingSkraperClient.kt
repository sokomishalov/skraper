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
@file:Suppress(
        "BlockingMethodInNonBlockingContext"
)

package ru.sokomishalov.skraper.client.jdk

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import ru.sokomishalov.skraper.SkraperClient
import ru.sokomishalov.skraper.client.HttpMethodType
import ru.sokomishalov.skraper.internal.net.openRedirectableStream
import ru.sokomishalov.skraper.internal.net.request
import ru.sokomishalov.skraper.model.URLString
import java.io.File
import java.net.URL
import java.nio.channels.Channels


/**
 * You should not use this implementation
 * @author sokomishalov
 */
object DefaultBlockingSkraperClient : SkraperClient {

    override suspend fun request(
            url: URLString,
            method: HttpMethodType,
            headers: Map<String, String>,
            body: ByteArray?
    ): ByteArray? {
        return withContext(IO) {
            URL(url).request(
                    method = method,
                    headers = headers,
                    body = body
            )
        }
    }

    override suspend fun download(
            url: URLString,
            destFile: File
    ) {
        withContext(IO) {
            Channels.newChannel(URL(url).openRedirectableStream()).use { rbc ->
                destFile.outputStream().use { fos ->
                    fos.channel.transferFrom(rbc, 0, Long.MAX_VALUE)
                    destFile.absolutePath
                }
            }
        }
    }
}