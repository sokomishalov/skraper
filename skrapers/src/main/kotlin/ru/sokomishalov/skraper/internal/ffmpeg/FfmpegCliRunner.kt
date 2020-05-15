@file:Suppress("BlockingMethodInNonBlockingContext")

package ru.sokomishalov.skraper.internal.ffmpeg

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Duration


/**
 * @author sokomishalov
 */
class FfmpegCliRunner(
        private val processLivenessCheckInterval: Duration = Duration.ofMillis(50)
) : FfmpegRunner {

    init {
        checkFfmpegExistence()
    }

    override suspend fun run(cmd: String, timeout: Duration): Int {
        val process = Runtime
                .getRuntime()
                .exec("ffmpeg $cmd")

        withTimeoutOrNull(timeout.toMillis()) {
            while (process.isAlive) {
                delay(processLivenessCheckInterval.toMillis())
            }
        }

        return process.exitValue()
    }

    private fun checkFfmpegExistence() {
        runBlocking {
            run(cmd = "-version", timeout = Duration.ofSeconds(1)).let { code ->
                if (code != 0) System.err.println("`ffmpeg` is not present in OS, some functions may work unreliably")
            }
        }
    }
}