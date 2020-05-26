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
 * @param media item to download
 * @param destDir destination file or directory for media
 * @param filename custom destination file name without extension
 * @param ffmpegRunner custom ffmpeg runner (for downloading m3u8 files)
 */
suspend fun Skraper.Companion.download(
        media: Media,
        destDir: File,
        filename: String = media.extractFileNameWithoutExtension(),
        client: SkraperClient = DefaultBlockingSkraperClient,
        skrapers: List<Skraper> = knownList(client),
        ffmpegRunner: FfmpegRunner = FfmpegCliRunner()
): File {

    val (directMediaUrl, extension) = skrapers.lookForDirectMediaLinkRecursively(media)

    val destFile = File("${destDir.absolutePath}$separator${filename}.${extension}").apply { destDir.mkdirs() }

    return when (extension) {

        // m3u8 download and convert to mp4 with ffmpeg
        "m3u8" -> {
            val destFileMp4Path = destFile.absolutePath.replace("m3u8", "mp4")
            val cmd = "-i $directMediaUrl -c copy -bsf:a aac_adtstoasc $destFileMp4Path"

            ffmpegRunner.run(cmd)

            File(destFileMp4Path)
        }

        // webm download and convert to mp4 with ffmpeg
        "webm" -> {
            val destFileMp4Path = destFile.absolutePath.replace("webm", "mp4")
            val cmd = "-i $directMediaUrl -strict experimental $destFileMp4Path"

            ffmpegRunner.run(cmd)

            File(destFileMp4Path)
        }

        // otherwise try to download as is
        else -> {
            client.download(url = directMediaUrl, destFile = destFile)
            destFile
        }
    }
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

private suspend fun List<Skraper>.lookForDirectMediaLinkRecursively(media: Media, recursionDepth: Int = 2): Pair<URLString, String> {
    return when {
        // direct media url
        media.url
                .path
                .substringAfterLast("/")
                .substringAfterLast(".", "")
                .isNotEmpty() -> media.url to media.extractFileExtension()

        // otherwise
        else -> {
            find { it.canResolve(media) }
                    ?.resolve(media)
                    ?.run {
                        when {
                            recursionDepth > 0 -> lookForDirectMediaLinkRecursively(media = this, recursionDepth = recursionDepth - 1)
                            else -> url to extractFileExtension()
                        }
                    }
                    ?: media.url to media.extractFileExtension()
        }
    }
}

private fun Media.extractFileExtension(): String {
    val filename = url.path

    return when (this) {
        is Image -> filename.substringAfterLast(".", "png")
        is Video -> filename.substringAfterLast(".", "mp4")
        is Audio -> filename.substringAfterLast(".", "mp3")
        is Article -> ""
    }
}

private fun Media.extractFileNameWithoutExtension(): String {
    return url
            .path
            .substringAfterLast("/")
            .substringBeforeLast(".")
}
