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
@file:JvmName("Skrapers")
@file:Suppress("unused")

package ru.sokomishalov.skraper

import ru.sokomishalov.skraper.client.HttpRequest
import ru.sokomishalov.skraper.client.SkraperClient
import ru.sokomishalov.skraper.client.jdk.DefaultBlockingSkraperClient
import ru.sokomishalov.skraper.internal.ffmpeg.FfmpegCliRunner
import ru.sokomishalov.skraper.internal.ffmpeg.FfmpegRunner
import ru.sokomishalov.skraper.internal.net.path
import ru.sokomishalov.skraper.model.Audio
import ru.sokomishalov.skraper.model.Image
import ru.sokomishalov.skraper.model.Media
import ru.sokomishalov.skraper.model.Video
import ru.sokomishalov.skraper.provider.facebook.FacebookSkraper
import ru.sokomishalov.skraper.provider.flickr.FlickrSkraper
import ru.sokomishalov.skraper.provider.ifunny.IFunnySkraper
import ru.sokomishalov.skraper.provider.instagram.InstagramSkraper
import ru.sokomishalov.skraper.provider.ninegag.NinegagSkraper
import ru.sokomishalov.skraper.provider.pikabu.PikabuSkraper
import ru.sokomishalov.skraper.provider.pinterest.PinterestSkraper
import ru.sokomishalov.skraper.provider.reddit.RedditSkraper
import ru.sokomishalov.skraper.provider.telegram.TelegramSkraper
import ru.sokomishalov.skraper.provider.tiktok.TikTokSkraper
import ru.sokomishalov.skraper.provider.tumblr.TumblrSkraper
import ru.sokomishalov.skraper.provider.twitch.TwitchSkraper
import ru.sokomishalov.skraper.provider.twitter.TwitterSkraper
import ru.sokomishalov.skraper.provider.vk.VkSkraper
import ru.sokomishalov.skraper.provider.youtube.YoutubeSkraper
import java.io.File

object Skrapers {

    /**
     * @return list of all available skrapers
     */
    fun available(): List<Skraper> {
        return providers.toList()
    }

    /**
     * @param url potential provider relative url
     * @return skraper which supports this url or null if none of skrapers supports it
     */
    fun suitable(url: String): Skraper? {
        return providers.find { it.supports(url) }
    }

    /**
     * Convert provider relative media to downloadable media
     * @param media item to resolve
     * @return media with direct link
     */
    suspend fun resolve(media: Media): Media {
        return when {
            // direct media url
            media.url
                .path
                .substringAfterLast("/")
                .substringAfterLast(".", "")
                .isNotEmpty() -> media

            // otherwise
            else -> {
                suitable(media.url)
                    ?.resolve(media)
                    ?.run {
                        when {
                            url != media.url -> resolve(media = this)
                            else -> when (media) {
                                is Image -> media.copy(url = url)
                                is Video -> media.copy(url = url)
                                is Audio -> media.copy(url = url)
                            }
                        }
                    }
                    ?: media
            }
        }
    }

    /**
     * Downloads media
     * @param media item to download
     * @param destDir destination directory for media
     * @param filename custom destination file name without extension
     */
    suspend fun download(
        media: Media,
        destDir: File,
        filename: String = media.extractFileNameWithoutExtension()
    ): File {
        val resolved = resolve(media)
        val extension = resolved.extractFileExtension()

        destDir.mkdirs()
        val destFile = File("${destDir.absolutePath}${File.separator}${filename}.${extension}")

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
                providers.random().client.download(HttpRequest(url = resolved.url), destFile = destFile)
                destFile
            }
        }
    }

    /**
     * Set client for all available skrapers
     * @param client client to set
     */
    fun setClient(client: SkraperClient) {
        this.providers = implementations(client)
    }

    /**
     * Set ffmpeg runner
     * @param ffmpegRunner ffmpeg runner implementation
     */
    fun setFfmpegRunner(ffmpegRunner: FfmpegRunner) {
        this.ffmpegRunner = ffmpegRunner
    }


    private fun Media.extractFileExtension(): String {
        val filename = url.path

        return when (this) {
            is Image -> filename.substringAfterLast(".", "png")
            is Video -> filename.substringAfterLast(".", "mp4")
            is Audio -> filename.substringAfterLast(".", "mp3")
        }
    }

    private fun Media.extractFileNameWithoutExtension(): String {
        return url.path.substringAfterLast("/").substringBeforeLast(".")
    }

    private fun implementations(client: SkraperClient): List<Skraper> = listOf(
        FacebookSkraper(client),
        InstagramSkraper(client),
        TwitterSkraper(client),
        YoutubeSkraper(client),
        TikTokSkraper(client),
        TelegramSkraper(client),
        TwitchSkraper(client),
        RedditSkraper(client),
        NinegagSkraper(client),
        PinterestSkraper(client),
        FlickrSkraper(client),
        TumblrSkraper(client),
        IFunnySkraper(client),
        VkSkraper(client),
        PikabuSkraper(client)
    )

    private var providers: List<Skraper> = implementations(DefaultBlockingSkraperClient)
    private var ffmpegRunner: FfmpegRunner = FfmpegCliRunner()
}