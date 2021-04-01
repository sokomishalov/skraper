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
@file:Suppress(
    "BlockingMethodInNonBlockingContext"
)

package ru.sokomishalov.skraper.internal.net

import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import ru.sokomishalov.skraper.client.HttpMethodType
import ru.sokomishalov.skraper.client.HttpMethodType.GET
import ru.sokomishalov.skraper.model.URLString
import java.io.DataOutputStream
import java.net.HttpURLConnection
import java.net.HttpURLConnection.*
import java.net.URL


/**
 * @author sokomishalov
 */

@PublishedApi
internal suspend fun URL.openRedirectableConnection(
    method: HttpMethodType = GET,
    headers: Map<String, String> = emptyMap(),
    body: ByteArray? = null
): HttpURLConnection = withContext(IO) {
    val conn = openConnection() as HttpURLConnection

    conn.applyData(method, headers, body)

    val status = conn.responseCode

    when {
        status != HTTP_OK && status in listOf(HTTP_MOVED_TEMP, HTTP_MOVED_PERM, HTTP_SEE_OTHER) -> {
            val newConn = URL(conn.getHeaderField("Location")).openConnection() as HttpURLConnection
            newConn.apply {
                setRequestProperty("Cookie", conn.getHeaderField("Set-Cookie"))
                applyData(method, headers, body)
            }
            newConn
        }
        else -> conn
    }
}

val URLString.path: String
    get() = URL(this).path

val URLString.host: String
    get() = URL(this).host

private fun HttpURLConnection.applyData(
    method: HttpMethodType,
    headers: Map<String, String>,
    body: ByteArray?
) {
    requestMethod = method.name
    headers.forEach { (k, v) -> addRequestProperty(k, v) }
    body?.let {
        doOutput = true
        DataOutputStream(outputStream).use { wr -> wr.write(it) }
    }
    connectTimeout = 5_000
    readTimeout = 5_000
}