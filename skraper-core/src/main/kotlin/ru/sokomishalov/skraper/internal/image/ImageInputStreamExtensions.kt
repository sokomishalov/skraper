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

    if (c1 == 'G'.toInt() && c2 == 'I'.toInt() && c3 == 'F'.toInt()) { // GIF
        skip(3)
        width = this.readInt(2, false)
        height = this.readInt(2, false)
        mimeType = "image/gif"
    } else if (c1 == 0xFF && c2 == 0xD8) { // JPG
        while (c3 == 255) {
            val marker = read()
            val len = this.readInt(2, true)
            if (marker == 192 || marker == 193 || marker == 194) {
                skip(1)
                height = this.readInt(2, true)
                width = this.readInt(2, true)
                mimeType = "image/jpeg"
                break
            }
            skip(len - 2.toLong())
            c3 = read()
        }
    } else if (c1 == 137 && c2 == 80 && c3 == 78) { // PNG
        skip(15)
        width = this.readInt(2, true)
        skip(2)
        height = this.readInt(2, true)
        mimeType = "image/png"
    } else if (c1 == 66 && c2 == 77) { // BMP
        skip(15)
        width = this.readInt(2, false)
        skip(2)
        height = this.readInt(2, false)
        mimeType = "image/bmp"
    } else {
        val c4 = read()
        if (c1 == 'M'.toInt() && c2 == 'M'.toInt() && c3 == 0 && c4 == 42 || c1 == 'I'.toInt() && c2 == 'I'.toInt() && c3 == 42 && c4 == 0) { //TIFF
            val bigEndian = c1 == 'M'.toInt()
            var ifd = 0
            val entries: Int
            ifd = this.readInt(4, bigEndian)
            skip(ifd - 8.toLong())
            entries = this.readInt(2, bigEndian)
            for (i in 1..entries) {
                val tag = this.readInt(2, bigEndian)
                val fieldType = this.readInt(2, bigEndian)
                val count = this.readInt(4, bigEndian).toLong()
                var valOffset: Int
                if (fieldType == 3 || fieldType == 8) {
                    valOffset = this.readInt(2, bigEndian)
                    skip(2)
                } else {
                    valOffset = this.readInt(4, bigEndian)
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