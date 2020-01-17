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
package ru.sokomishalov.skraper.client.jdk

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import ru.sokomishalov.skraper.SkraperClient
import java.net.HttpURLConnection
import java.net.HttpURLConnection.*
import java.net.URL


/**
 * @author sokomishalov
 *
 * appreciation to mkyong
 * @see <a href="https://mkyong.com/java/java-httpurlconnection-follow-redirect-example/">link</a>
 */
class DefaultBlockingSkraperClient : SkraperClient {

    override suspend fun fetch(url: String): ByteArray? {
        return withContext(IO) {
            val conn = URL(url).openConnection() as HttpURLConnection

            conn.apply {
                readTimeout = 5000
            }

            val status = conn.responseCode

            if (status != HTTP_OK && status in listOf(HTTP_MOVED_TEMP, HTTP_MOVED_PERM, HTTP_SEE_OTHER)) {
                val newConn = URL(conn.getHeaderField("Location")).openConnection() as HttpURLConnection
                newConn.apply {
                    setRequestProperty("Cookie", conn.getHeaderField("Set-Cookie"))
                }
                newConn.inputStream.readBytes()
            } else {
                conn.inputStream.readBytes()
            }

        }
    }

}