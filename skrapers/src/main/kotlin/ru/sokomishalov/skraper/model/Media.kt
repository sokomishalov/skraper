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
package ru.sokomishalov.skraper.model

import java.time.Duration

/**
 * Represents a media item.
 * @property url media url
 */
sealed class Media {
    abstract val url: URLString
}

/**
 * Represents an image.
 * @property url image url
 * @property aspectRatio width to height ratio
 */
data class Image(
    override val url: URLString,
    val aspectRatio: Double? = null
) : Media()

/**
 * Represents an image.
 * @property url video url
 * @property aspectRatio width to height ratio
 * @property thumbnail thumb
 * @property duration video duration
 */
data class Video(
    override val url: URLString,
    val aspectRatio: Double? = null,
    val thumbnail: Image? = null,
    val duration: Duration? = null
) : Media()

/**
 * Represents an audio.
 * @property url audio url
 * @property duration audio duration
 */
data class Audio(
    override val url: URLString,
    val duration: Duration? = null
) : Media()
