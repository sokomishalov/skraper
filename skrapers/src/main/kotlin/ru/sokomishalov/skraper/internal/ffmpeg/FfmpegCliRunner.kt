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
@file:Suppress("BlockingMethodInNonBlockingContext")

package ru.sokomishalov.skraper.internal.ffmpeg

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.InputStream
import java.time.Duration


/**
 * @author sokomishalov
 */
open class FfmpegCliRunner @JvmOverloads constructor(
    private val processLivenessCheckInterval: Duration = Duration.ofMillis(50)
) : FfmpegRunner {

    private companion object {
        init {
            FfmpegCliRunner().checkFfmpegExistence()
        }
    }

    override suspend fun run(
        cmd: String,
        timeout: Duration,
        stdin: (InputStream) -> Unit
    ): Int {
        val process = Runtime.getRuntime().exec("ffmpeg $cmd")

        withTimeoutOrNull(timeout.toMillis()) {
            stdin(process.inputStream)
            process.await()
        }

        return runCatching { process.exitValue() }.getOrElse { -1 }
    }

    private suspend fun Process.await() {
        while (isAlive) delay(processLivenessCheckInterval.toMillis())
    }

    private fun checkFfmpegExistence() {
        runBlocking {
            run(cmd = "-version", timeout = Duration.ofSeconds(2)).let { code ->
                if (code != 0) System.err.println("`ffmpeg` is not present in OS, some functions may work unreliably")
            }
        }
    }
}