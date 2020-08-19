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
package ru.sokomishalov.skraper

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import ru.sokomishalov.skraper.client.HttpMethodType
import ru.sokomishalov.skraper.client.HttpMethodType.GET
import ru.sokomishalov.skraper.internal.consts.DEFAULT_USER_AGENT
import ru.sokomishalov.skraper.model.URLString
import java.io.File

/**
 * @author sokomishalov
 */
interface SkraperClient {

    /**
     * execute http request
     */
    suspend fun request(
            url: URLString,
            method: HttpMethodType = GET,
            headers: Map<String, String> = mapOf("User-Agent" to DEFAULT_USER_AGENT),
            body: ByteArray? = null
    ): ByteArray?

    /**
     * download files
     */
    suspend fun download(
            url: URLString,
            destFile: File
    ) {
        request(url = url)?.let { withContext(IO) { destFile.writeBytes(it) } }
    }

}