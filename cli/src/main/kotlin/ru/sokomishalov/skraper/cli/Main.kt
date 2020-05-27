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

package ru.sokomishalov.skraper.cli

import com.andreapivetta.kolor.cyan
import com.andreapivetta.kolor.green
import com.andreapivetta.kolor.magenta
import com.andreapivetta.kolor.red
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.cli.Args.Companion.DEFAULT_CLIENT
import ru.sokomishalov.skraper.download
import ru.sokomishalov.skraper.model.Post
import ru.sokomishalov.skraper.name
import java.io.File
import java.time.LocalDateTime.now
import java.time.format.DateTimeFormatter.ofPattern
import java.util.concurrent.Executors
import kotlin.system.exitProcess
import kotlin.text.Charsets.UTF_8


fun main(args: Array<String>) = mainBody(columns = 100) {
    val parsedArgs = ArgParser(args = args.ifEmpty { arrayOf("--help") }).parseInto(::Args)

    println("${"Skraper".green()} ${"v.0.4.2".magenta()} started")

    val posts = runBlocking {
        parsedArgs.skraper.getPosts(
                path = "/${parsedArgs.path.removePrefix("/")}",
                limit = parsedArgs.amount
        )
    }

    when {
        parsedArgs.onlyMedia -> posts.persistMedia(parsedArgs)
        else -> posts.persistMeta(parsedArgs)
    }
}

private fun List<Post>.persistMedia(parsedArgs: Args) {
    val provider = parsedArgs.skraper.name
    val requestedPath = parsedArgs.path
    val root = when {
        parsedArgs.output.isFile -> parsedArgs.output.parentFile.absolutePath
        else -> parsedArgs.output.absolutePath
    }
    val targetDir = File("${root}/${provider}/${requestedPath}").apply { mkdirs() }

    runBlocking(context = Executors.newFixedThreadPool(parsedArgs.parallelDownloads).asCoroutineDispatcher()) {
        flatMap { post ->
            post.media.mapIndexed { index, media ->
                async {
                    runCatching {
                        Skraper.download(
                                media = media,
                                destDir = targetDir,
                                filename = when (post.media.size) {
                                    1 -> post.id
                                    else -> "${post.id}_${index + 1}"
                                },
                                client = DEFAULT_CLIENT
                        )
                    }.onSuccess { path ->
                        println(path)
                    }.onFailure { thr ->
                        println("Cannot download ${media.url} , Reason: ${thr.toString().red()}")
                    }
                }
            }
        }.awaitAll()
    }

    exitProcess(1)
}

private fun List<Post>.persistMeta(parsedArgs: Args) {
    val provider = parsedArgs.skraper.name
    val requestedPath = parsedArgs.path

    val content = with(parsedArgs.outputType) { serialize() }

    val fileToWrite = when {
        parsedArgs.output.isFile -> parsedArgs.output
        else -> {
            val root = parsedArgs.output.absolutePath
            val now = now().format(ofPattern("ddMMyyyy'_'hhmmss"))
            val ext = parsedArgs.outputType.extension

            File("${root}/${provider}/${requestedPath}_${now}.${ext}")
        }
    }

    fileToWrite
            .apply { parentFile.mkdirs() }
            .writeText(text = content, charset = UTF_8)

    println(fileToWrite.path.cyan())
}
