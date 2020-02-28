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
package ru.sokomishalov.skraper.internal.serialization

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_SINGLE_QUOTES
import com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_UNQUOTED_FIELD_NAMES
import com.fasterxml.jackson.databind.DeserializationFeature.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature.*
import com.fasterxml.jackson.databind.node.MissingNode
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext
import kotlin.text.Charsets.UTF_8


/**
 * @author sokomishalov
 */

private val INTERNAL_SKRAPER_OBJECT_MAPPER: ObjectMapper by lazy {
    ObjectMapper()
            .enable(
                    ALLOW_SINGLE_QUOTES,
                    ALLOW_UNQUOTED_FIELD_NAMES
            )
            .enable(
                    READ_UNKNOWN_ENUM_VALUES_USING_DEFAULT_VALUE,
                    READ_ENUMS_USING_TO_STRING,
                    ACCEPT_SINGLE_VALUE_AS_ARRAY,
                    ACCEPT_EMPTY_STRING_AS_NULL_OBJECT,
                    ACCEPT_EMPTY_ARRAY_AS_NULL_OBJECT
            )
            .enable(
                    ACCEPT_CASE_INSENSITIVE_ENUMS
            )
            .enable(
                    WRITE_ENUMS_USING_TO_STRING
            )
            .disable(
                    FAIL_ON_EMPTY_BEANS,
                    WRITE_DATES_AS_TIMESTAMPS
            )
            .disable(
                    ADJUST_DATES_TO_CONTEXT_TIME_ZONE,
                    FAIL_ON_UNKNOWN_PROPERTIES,
                    FAIL_ON_INVALID_SUBTYPE,
                    FAIL_ON_IGNORED_PROPERTIES,
                    FAIL_ON_UNRESOLVED_OBJECT_IDS,
                    FAIL_ON_TRAILING_TOKENS
            )
            .setSerializationInclusion(NON_NULL)
}

@PublishedApi
internal suspend fun ByteArray?.aReadJsonNodes(): JsonNode? {
    return this?.let {
        withContext(IO) {
            INTERNAL_SKRAPER_OBJECT_MAPPER.readTree(it)
        }
    }
}

@PublishedApi
internal suspend fun String?.aReadJsonNodes(): JsonNode? {
    return this
            ?.toByteArray(UTF_8)
            .aReadJsonNodes()
}

internal fun JsonNode.getByKeyContaining(keyPart: String): JsonNode? {
    return fields()
            ?.asSequence()
            ?.find { it.key.contains(keyPart) }
            ?.value
}

internal fun JsonNode.getFirstByPath(vararg paths: String, delimiter: String = "."): JsonNode? {
    return paths
            .map { this.at("/${it.replace(delimiter, "/")}") }
            .firstOrNull { it !is MissingNode }
}