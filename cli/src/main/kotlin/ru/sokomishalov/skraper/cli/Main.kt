@file:Suppress("unused")

package ru.sokomishalov.skraper.cli

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.databind.JsonSerializer
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.json.JsonMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.dataformat.csv.CsvMapper
import com.fasterxml.jackson.dataformat.csv.CsvSchema
import com.fasterxml.jackson.dataformat.xml.XmlMapper
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
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

fun main(args: Array<String>): Unit = mainBody {
    val parsedArgs = ArgParser(when {
        args.isEmpty() -> arrayOf("--help")
        else -> args
    }).parseInto(::Args)

    val skraper = parsedArgs.provider.skraper

    val path = when {
        parsedArgs.path.startsWith("/") -> parsedArgs.path
        else -> "/${parsedArgs.path}"
    }

    val posts = runBlocking { skraper.getPosts(path = path, limit = parsedArgs.amount) }

    val content = when (parsedArgs.outputType) {
        LOG -> posts.joinToString("\n") { it.toString() }.also { println(it) }
        JSON -> JsonMapper().apply { registerKotlinModule() }.writerWithDefaultPrettyPrinter().writeValueAsString(posts)
        XML -> XmlMapper().apply { registerKotlinModule() }.writerWithDefaultPrettyPrinter().writeValueAsString(posts)
        YAML -> YAMLMapper().apply { registerKotlinModule() }.writerWithDefaultPrettyPrinter().writeValueAsString(posts)
        CSV -> {
            val mapper = CsvMapper().apply {
                registerKotlinModule()
                registerModule(SimpleModule().apply {
                    addSerializer(Post::class.java, object : JsonSerializer<Post>() {
                        override fun serialize(item: Post, jgen: JsonGenerator, serializerProvider: SerializerProvider) {
                            jgen.writeStartObject()
                            jgen.writeStringField("ID", item.id)
                            jgen.writeStringField("Text", item.text)
                            jgen.writeStringField("Published at", item.publishedAt?.toString(10))
                            jgen.writeNumberField("Rating", item.rating ?: 0)
                            jgen.writeNumberField("Comments count", item.commentsCount ?: 0)
                            jgen.writeStringField("Attachments", item.attachments.joinToString("   ") { it.url })
                            jgen.writeEndObject()
                        }
                    })
                })
            }

            val schema = CsvSchema
                    .builder()
                    .addColumn("ID")
                    .addColumn("Text")
                    .addColumn("Published at")
                    .addColumn("Rating")
                    .addColumn("Comments count")
                    .addColumn("Attachments")
                    .build()
                    .withHeader()

            val writer = mapper.writer(schema)

            writer.writeValueAsString(posts)
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
    fileToWrite.writeText(text = content, charset = UTF_8)
}