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

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration


/**
 * @author sokomishalov
 */
open class FfmpegCliRunner @JvmOverloads constructor(
    private val processTimeout: Duration = Duration.ofHours(1),
    private val processLivenessCheckInterval: Duration = Duration.ofMillis(100),
) : FfmpegRunner {

    init {
        checkFfmpegExistence()
    }

    override suspend fun run(cmd: String): Int {
        return runCatching {
            with(Runtime.getRuntime().exec("ffmpeg $cmd")) {
                await()
                exitValue()
            }
        }.getOrElse {
            -1
        }
    }

    private suspend fun Process.await() {
        withTimeoutOrNull(processTimeout.toMillis()) {
            while (isAlive) delay(processLivenessCheckInterval.toMillis())
        }
    }

    private fun checkFfmpegExistence() {
        GlobalScope.launch {
            val code = run("-version")
            if (code != 0) {
                println("`ffmpeg` is not present in OS, some functions may work unreliably")
            }
        }
    }
}