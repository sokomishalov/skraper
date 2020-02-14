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
package ru.sokomishalov.skraper.internal.serialization

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.databind.DeserializationFeature.*
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.MapperFeature.ACCEPT_CASE_INSENSITIVE_ENUMS
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature.*
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.withContext

/**
 * @author sokomishalov
 */

suspend fun ByteArray?.aReadJsonNodes(): JsonNode {
    return withContext(IO) {
        INTERNAL_SKRAPER_OBJECT_MAPPER.readTree(this@aReadJsonNodes)
    }
}

private val INTERNAL_SKRAPER_OBJECT_MAPPER: ObjectMapper by lazy {
    ObjectMapper()
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
                    FAIL_ON_UNKNOWN_PROPERTIES
            )
            .setSerializationInclusion(NON_NULL)
}