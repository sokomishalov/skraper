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
package ru.sokomishalov.skraper.cli

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.ObjectWriter
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import ru.sokomishalov.skraper.model.Post
import java.time.ZoneId
import java.time.format.DateTimeFormatter.ISO_INSTANT

/**
 * @author sokomishalov
 */
enum class Serialization(val extension: String) {

    LOG("log") {
        override fun List<Post>.serialize(): String {
            return joinToString("\n") { it.toString() }.also { println(it) }
        }
    },

    JSON("json") {
        override fun List<Post>.serialize(): String {
            return JsonMapper().serialize(this)
        }
    },

    XML("xml") {
        override fun List<Post>.serialize(): String {
            return XmlMapper().serialize(XmlPostsWrapper(this))
        }
    },

    YAML("yaml") {
        override fun List<Post>.serialize(): String {
            return YAMLMapper().serialize(this)
        }
    },

    CSV("csv") {
        override fun List<Post>.serialize(): String {
            return CsvMapper()
                .registerModule(
                    SimpleModule().apply {
                        addSerializer(Post::class.java, object : JsonSerializer<Post>() {
                            override fun serialize(item: Post, jgen: JsonGenerator, serializerProvider: SerializerProvider) {
                                with(jgen) {
                                    writeStartObject()
                                    writeStringField("ID", item.id)
                                    writeStringField("Text", item.text)
                                    writeStringField("Published at", item.publishedAt?.let { ISO_INSTANT.withZone(ZoneId.systemDefault()).format(it) })
                                    writeStringField("Rating", item.statistics.likes?.toString(10).orEmpty())
                                    writeStringField("Comments count", item.statistics.comments?.toString(10).orEmpty())
                                    writeStringField("Views count", item.statistics.views?.toString(10).orEmpty())
                                    writeStringField("Media", item.media.joinToString("   ") { it.url })
                                    writeEndObject()
                                }
                            }
                        })
                    }
                )
                .writer(
                    CsvSchema
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
                .serialize(this)
        }
    };

    abstract fun List<Post>.serialize(): String

    internal fun ObjectMapper.serialize(data: Any): String {
        return this
            .registerKotlinModule()
            .registerModule(JavaTimeModule())
            .registerModule(Jdk8Module())
            .writerWithDefaultPrettyPrinter()
            .serialize(data)
    }

    internal fun ObjectWriter.serialize(posts: Any): String {
        return writeValueAsString(posts)
    }

    @JacksonXmlRootElement(localName = "posts")
    private data class XmlPostsWrapper<T>(@JacksonXmlElementWrapper(useWrapping = false) val post: List<T>)
}