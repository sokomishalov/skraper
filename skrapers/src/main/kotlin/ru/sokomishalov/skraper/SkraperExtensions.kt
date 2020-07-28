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

import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.internal.ffmpeg.FfmpegCliRunner
import ru.sokomishalov.skraper.internal.ffmpeg.FfmpegRunner
import ru.sokomishalov.skraper.internal.net.path
import ru.sokomishalov.skraper.model.*
import ru.sokomishalov.skraper.provider.facebook.FacebookSkraper
import ru.sokomishalov.skraper.provider.flickr.FlickrSkraper
import ru.sokomishalov.skraper.provider.ifunny.IFunnySkraper
import ru.sokomishalov.skraper.provider.instagram.InstagramSkraper
import ru.sokomishalov.skraper.provider.ninegag.NinegagSkraper
import ru.sokomishalov.skraper.provider.pikabu.PikabuSkraper
import ru.sokomishalov.skraper.provider.pinterest.PinterestSkraper
import ru.sokomishalov.skraper.provider.reddit.RedditSkraper
import ru.sokomishalov.skraper.provider.tumblr.TumblrSkraper
import ru.sokomishalov.skraper.provider.twitch.TwitchSkraper
import ru.sokomishalov.skraper.provider.twitter.TwitterSkraper
import ru.sokomishalov.skraper.provider.vk.VkSkraper
import ru.sokomishalov.skraper.provider.youtube.YoutubeSkraper
import java.io.File
import java.io.File.separator


/**
 * Downloads media
 * @param media item to download
 * @param destDir destination directory for media
 * @param filename custom destination file name without extension
 * @param client skraper client
 * @param skrapers all known skrapers
 * @param ffmpegRunner custom ffmpeg runner (for converting m3u8 and webm files)
 */
suspend fun Skraper.Companion.download(
        media: Media,
        destDir: File,
        filename: String = media.extractFileNameWithoutExtension(),
        client: SkraperClient = DefaultBlockingSkraperClient,
        skrapers: List<Skraper> = knownList(client),
        ffmpegRunner: FfmpegRunner = FfmpegCliRunner()
): File {
    val resolved = skrapers.lookForDirectMediaRecursively(media)
    val extension = resolved.extractFileExtension()

    val destFile = File("${destDir.absolutePath}$separator${filename}.${extension}").apply { destDir.mkdirs() }

    return when (extension) {

        // m3u8 download and convert to mp4 with ffmpeg
        "m3u8" -> {
            val destFileMp4Path = destFile.absolutePath.replace("m3u8", "mp4")
            val cmd = "-i ${resolved.url} -c copy -bsf:a aac_adtstoasc $destFileMp4Path"

            ffmpegRunner.run(cmd)

            File(destFileMp4Path)
        }

        // webm download and convert to mp4 with ffmpeg
        "webm" -> {
            val destFileMp4Path = destFile.absolutePath.replace("webm", "mp4")
            val cmd = "-i ${resolved.url} -strict experimental $destFileMp4Path"

            ffmpegRunner.run(cmd)

            File(destFileMp4Path)
        }

        // otherwise try to download as is
        else -> {
            client.download(url = resolved.url, destFile = destFile)
            destFile
        }
    }
}

/**
 * Convert provider relative media to downloadable media
 * @param media item to resolve
 * @param client skraper client
 * @param skrapers all known skrapers
 */
suspend fun Skraper.Companion.resolve(
        media: Media,
        client: SkraperClient = DefaultBlockingSkraperClient,
        skrapers: List<Skraper> = knownList(client)
): Media {
    return skrapers.lookForDirectMediaRecursively(media)
}

fun Skraper.Companion.knownList(client: SkraperClient): List<Skraper> = listOf(
        FacebookSkraper(client = client),
        InstagramSkraper(client = client),
        TwitterSkraper(client = client),
        YoutubeSkraper(client = client),
        TwitchSkraper(client = client),
        RedditSkraper(client = client),
        NinegagSkraper(client = client),
        PinterestSkraper(client = client),
        FlickrSkraper(client = client),
        TumblrSkraper(client = client),
        IFunnySkraper(client = client),
        VkSkraper(client = client),
        PikabuSkraper(client = client)
)

inline val Skraper.name: String
    get() = this::class.java.simpleName.removeSuffix("Skraper").toLowerCase()


private suspend fun List<Skraper>.lookForDirectMediaRecursively(media: Media, recursionDepth: Int = 2): Media {
    return when {
        // direct media url
        media.url
                .path
                .substringAfterLast("/")
                .substringAfterLast(".", "")
                .isNotEmpty() -> media

        // otherwise
        else -> {
            find { it.supports(media.url) }
                    ?.resolve(media)
                    ?.run {
                        when {
                            recursionDepth > 0 && url != media.url -> lookForDirectMediaRecursively(media = this, recursionDepth = recursionDepth - 1)
                            else -> when (media) {
                                is Image -> media.copy(url = url)
                                is Video -> media.copy(url = url)
                                is Audio -> media.copy(url = url)
                                is UnknownLink -> media.copy(url = url)
                            }
                        }
                    }
                    ?: media
        }
    }
}

private fun Media.extractFileExtension(): String {
    val filename = url.path

    return when (this) {
        is Image -> filename.substringAfterLast(".", "png")
        is Video -> filename.substringAfterLast(".", "mp4")
        is Audio -> filename.substringAfterLast(".", "mp3")
        is UnknownLink -> ""
    }
}

private fun Media.extractFileNameWithoutExtension(): String {
    return url
            .path
            .substringAfterLast("/")
            .substringBeforeLast(".")
}
