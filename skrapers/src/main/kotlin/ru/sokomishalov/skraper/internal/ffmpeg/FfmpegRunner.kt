package ru.sokomishalov.skraper.internal.ffmpeg

import java.time.Duration


/**
 * @author sokomishalov
 */
interface FfmpegRunner {

    suspend fun run(cmd: String, timeout: Duration = Duration.ofHours(1)): Int

}