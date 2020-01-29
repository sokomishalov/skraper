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
@file:Suppress("UNUSED_VARIABLE")

package ru.sokomishalov.skraper.internal.image

import java.io.InputStream

/**
 *
 * Huge appreciation to jaimon mathew
 * @see <a href="http://blog.jaimon.co.uk/simpleimageinfo/SimpleImageInfo.java.html">link</a>
 */
internal fun InputStream.getRemoteImageInfo(): SimpleImageInfo = use {
    val c1 = read()
    val c2 = read()
    var c3 = read()

    var height: Int = -1
    var width: Int = -1
    var mimeType: String? = null

    when {
        // GIF
        c1 == 'G'.toInt() && c2 == 'I'.toInt() && c3 == 'F'.toInt() -> {
            skip(3)
            width = readInt(2, false)
            height = readInt(2, false)
            mimeType = "image/gif"
        }

        // JPG
        c1 == 0xFF && c2 == 0xD8 -> {
            while (c3 == 255) {
                val marker = read()
                val len = readInt(2, true)
                if (marker in (192..194)) {
                    skip(1)
                    height = readInt(2, true)
                    width = readInt(2, true)
                    mimeType = "image/jpeg"
                    break
                }
                skip(len - 2.toLong())
                c3 = read()
            }
        }

        // PNG
        c1 == 137 && c2 == 80 && c3 == 78 -> {
            skip(15)
            width = readInt(2, true)
            skip(2)
            height = readInt(2, true)
            mimeType = "image/png"
        }

        // BMP
        c1 == 66 && c2 == 77 -> {
            skip(15)
            width = readInt(2, false)
            skip(2)
            height = readInt(2, false)
            mimeType = "image/bmp"
        }

        // Unknown
        else -> {
            val c4 = read()

            when {
                //TIFF
                (c1 == 'M'.toInt() && c2 == 'M'.toInt() && c3 == 0 && c4 == 42) || (c1 == 'I'.toInt() && c2 == 'I'.toInt() && c3 == 42 && c4 == 0) -> {
                    val bigEndian = c1 == 'M'.toInt()
                    var ifd = 0
                    val entries: Int
                    ifd = readInt(4, bigEndian)
                    skip(ifd - 8.toLong())
                    entries = readInt(2, bigEndian)

                    for (i in 1..entries) {
                        val tag = readInt(2, bigEndian)
                        val fieldType = readInt(2, bigEndian)
                        val count = readInt(4, bigEndian).toLong()
                        var valOffset: Int
                        if (fieldType == 3 || fieldType == 8) {
                            valOffset = readInt(2, bigEndian)
                            skip(2)
                        } else {
                            valOffset = readInt(4, bigEndian)
                        }
                        if (tag == 256) {
                            width = valOffset
                        } else if (tag == 257) {
                            height = valOffset
                        }
                        if (width != -1 && height != -1) {
                            mimeType = "image/tiff"
                            break
                        }
                    }
                }
            }
        }
    }

    SimpleImageInfo(width = width, height = height, mimeType = mimeType)
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