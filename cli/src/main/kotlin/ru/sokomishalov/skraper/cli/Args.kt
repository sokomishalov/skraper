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

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import ru.sokomishalov.skraper.Skraper
import ru.sokomishalov.skraper.cli.Serialization.LOG
import ru.sokomishalov.skraper.client.ktor.KtorSkraperClient
import ru.sokomishalov.skraper.knownList
import ru.sokomishalov.skraper.name
import java.io.File

class Args(parser: ArgParser) {

    companion object {
        internal val DEFAULT_CLIENT = KtorSkraperClient()
        internal val SKRAPERS = Skraper.knownList(client = DEFAULT_CLIENT)
    }

    val skraper by parser.positional(
            name = "PROVIDER",
            help = "skraper provider, options: ${SKRAPERS.joinToString { it.name }}"
    ) {
        SKRAPERS
                .find { this == it.name }
                .let { requireNotNull(it) { "Unknown provider" } }
    }

    val path by parser.positional(
            name = "PATH",
            help = "path to user/community/channel/topic/trend"
    )

    val amount by parser.storing(
            "-n", "--limit",
            help = "posts limit (50 by default)"
    ) { toInt() }.default { 50 }

    val outputType by parser.storing(
            "-t", "--type",
            help = "output type, options: ${Serialization.values().joinToString().toLowerCase()}"
    ) { Serialization.valueOf(toUpperCase()) }.default { LOG }

    val output by parser.storing(
            "-o", "--output",
            help = "output path"
    ) { File(this) }.default { File("") }

    val onlyMedia by parser.flagging(
            "-m", "--media-only",
            help = "scrape media only"
    )

    val parallelDownloads by parser.storing(
            "--parallel-downloads",
            help = "amount of parallel downloads for media items if enabled flag --media-only (4 by default)"
    ) { toInt() }.default { 4 }
}