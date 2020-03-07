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
@file:Suppress("UNUSED_VARIABLE")

package ru.sokomishalov.skraper.internal.image

import java.io.InputStream

/**
 *
 * Huge appreciation to jaimon mathew
 * @see <a href="http://blog.jaimon.co.uk/simpleimageinfo/SimpleImageInfo.java.html">link</a>
 * @return width to height pair
 */
internal val InputStream.imageDimensions: Pair<Int, Int>
    get() = use {
        val c1 = read()
        val c2 = read()
        var c3 = read()

        return when {
            // GIF
            c1 == 'G'.toInt() && c2 == 'I'.toInt() && c3 == 'F'.toInt() -> {
                skip(3)
                val width = readInt(2, false)
                val height = readInt(2, false)
                width to height
            }

            // JPG
            c1 == 0xFF && c2 == 0xD8 -> {
                while (c3 == 255) {
                    val marker = read()
                    val len = readInt(2, true)
                    if (marker in (192..194)) {
                        skip(1)
                        val height = readInt(2, true)
                        val width = readInt(2, true)
                        return@use width to height
                    }
                    skip(len - 2.toLong())
                    c3 = read()
                }
                -1 to -1
            }

            // PNG
            c1 == 137 && c2 == 80 && c3 == 78 -> {
                skip(15)
                val width = readInt(2, true)
                skip(2)
                val height = readInt(2, true)
                width to height
            }

            // BMP
            c1 == 66 && c2 == 77 -> {
                skip(15)
                val width = readInt(2, false)
                skip(2)
                val height = readInt(2, false)
                width to height
            }

            // Unknown
            else -> {
                val c4 = read()

                when {
                    //TIFF
                    (c1 == 'M'.toInt() && c2 == 'M'.toInt() && c3 == 0 && c4 == 42) || (c1 == 'I'.toInt() && c2 == 'I'.toInt() && c3 == 42 && c4 == 0) -> {
                        val bigEndian = c1 == 'M'.toInt()
                        val ifd: Int
                        val entries: Int
                        ifd = readInt(4, bigEndian)
                        skip(ifd - 8.toLong())
                        entries = readInt(2, bigEndian)

                        var width = -1
                        var height = -1

                        for (i in 1..entries) {
                            val tag = readInt(2, bigEndian)
                            val fieldType = readInt(2, bigEndian)
                            val count = readInt(4, bigEndian).toLong()

                            val valOffset = when (fieldType) {
                                3, 8 -> readInt(2, bigEndian).apply { skip(2) }
                                else -> readInt(4, bigEndian)
                            }

                            when (tag) {
                                256 -> width = valOffset
                                257 -> height = valOffset
                            }
                        }

                        width to height
                    }
                    else -> -1 to -1
                }
            }
        }
    }

private fun InputStream.readInt(noOfBytes: Int, bigEndian: Boolean): Int {
    var ret = 0
    var sv = if (bigEndian) (noOfBytes - 1) * 8 else 0
    val cnt = if (bigEndian) -8 else 8
    repeat((0 until noOfBytes).count()) {
        ret = ret or read() shl sv
        sv += cnt
    }
    return ret
}