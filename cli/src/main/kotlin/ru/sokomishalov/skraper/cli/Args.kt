/**
 * Copyright 2019-2020 the original author or authors.
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
import ru.sokomishalov.skraper.cli.OutputType.LOG
import ru.sokomishalov.skraper.internal.consts.DEFAULT_POSTS_LIMIT
import java.io.File

class Args(parser: ArgParser) {
    val provider by parser.positional(
            name = "PROVIDER",
            help = "skraper provider, options: ${Provider.values().contentToString().toLowerCase()}"
    ) { Provider.valueOf(toUpperCase()) }

    val path by parser.positional(
            name = "PATH",
            help = "path to user/community/channel/topic/trend"
    )

    val amount by parser.storing(
            "-n", "--limit",
            help = "posts limit"
    ) { toInt() }.default(DEFAULT_POSTS_LIMIT)

    val outputType by parser.storing(
            "-t", "--type",
            help = "output type, options: ${OutputType.values().contentToString().toLowerCase()}"
    ) { OutputType.valueOf(toUpperCase()) }.default(LOG)

    val output by parser.storing(
            "-o", "--output",
            help = "output path"
    ) { File(this) }.default { File("") }
}