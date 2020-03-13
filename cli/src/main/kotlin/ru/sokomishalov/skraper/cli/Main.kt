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
@file:Suppress("unused")

package ru.sokomishalov.skraper.cli

import com.andreapivetta.kolor.cyan
import com.andreapivetta.kolor.green
import com.andreapivetta.kolor.magenta
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import kotlinx.coroutines.runBlocking
import ru.sokomishalov.skraper.cli.OutputType.*
import ru.sokomishalov.skraper.model.Post
import java.io.File
import java.time.LocalDateTime.now
import java.time.format.DateTimeFormatter.ofPattern
import kotlin.text.Charsets.UTF_8

fun main(args: Array<String>) = mainBody(columns = 150) {
    val parsedArgs = ArgParser(
            args = args.ifEmpty { arrayOf("--help") }
    ).parseInto(::Args)

    println("${"Skraper".green()} ${"v.0.2.3".magenta()} started")

    val posts = runBlocking {
        parsedArgs.provider.skraper.getPosts(
                path = "/${parsedArgs.path.removeSuffix("/")}",
                limit = parsedArgs.amount
        )
    }

    val content = when (parsedArgs.outputType) {
        LOG -> posts
                .joinToString("\n") { it.toString() }
                .also { println(it) }
        JSON -> JsonMapper()
                .registerModule(JavaTimeModule())
                .registerModule(Jdk8Module())
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(posts)
        XML -> XmlMapper()
                .registerModule(JavaTimeModule())
                .registerModule(Jdk8Module())
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(posts)
        YAML -> YAMLMapper()
                .registerModule(JavaTimeModule())
                .registerModule(Jdk8Module())
                .writerWithDefaultPrettyPrinter()
                .writeValueAsString(posts)
        CSV -> {
            CsvMapper()
                    .apply {
                        registerKotlinModule()
                        registerModule(JavaTimeModule())
                        registerModule(Jdk8Module())
                        registerModule(SimpleModule().apply {
                            addSerializer(Post::class.java, object : JsonSerializer<Post>() {
                                override fun serialize(item: Post, jgen: JsonGenerator, serializerProvider: SerializerProvider) {
                                    jgen.writeStartObject()
                                    jgen.writeStringField("ID", item.id)
                                    jgen.writeStringField("Text", item.text)
                                    jgen.writeStringField("Published at", item.publishedAt?.toString(10))
                                    jgen.writeStringField("Rating", item.rating?.toString(10).orEmpty())
                                    jgen.writeStringField("Comments count", item.commentsCount?.toString(10).orEmpty())
                                    jgen.writeStringField("Views count", item.viewsCount?.toString(10).orEmpty())
                                    jgen.writeStringField("Media", item.media.joinToString("   ") { it.url })
                                    jgen.writeEndObject()
                                }
                            })
                        })
                    }
                    .writer(CsvSchema
                            .builder()
                            .addColumn("ID")
                            .addColumn("Text")
                            .addColumn("Published at")
                            .addColumn("Rating")
                            .addColumn("Comments count")
                            .addColumn("Views count")
                            .addColumn("Media")
                            .build()
                            .withHeader()
                    )
                    .writeValueAsString(posts)

        }
    }

    val fileToWrite = when {
        parsedArgs.output.isFile -> parsedArgs.output
        else -> {
            val root = parsedArgs.output.absolutePath
            val provider = parsedArgs.provider.toString().toLowerCase()
            val requestedPath = parsedArgs.path
            val now = now().format(ofPattern("ddMMyyyy'_'hhmmss"))
            val ext = parsedArgs.outputType.extension

            File("${root}/${provider}/${requestedPath}_${now}${ext}")
        }
    }

    fileToWrite.parentFile.mkdirs()
    fileToWrite
            .apply { parentFile.mkdirs() }
            .also { println("Fetched ${posts.size.toString().green()} posts. Saved to: ${it.path.cyan()}") }
            .writeText(text = content, charset = UTF_8)
}