package ru.sokomishalov.skraper.internal.string

/**
 * muchas gracias
 * @see <a href="https://stackoverflow.com/a/49831779/5843129">link</a>
 *
 * @author sokomishalov
 */

@PublishedApi
internal fun String.unescapeJson(): String {
    val builder = StringBuilder()
    var i = 0
    while (i < length) {
        val delimiter = this[i]
        i++
        if (delimiter == '\\' && i < length) {
            val ch = this[i]
            i++
            when (ch) {
                '\\', '/', '"', '\'' -> builder.append(ch)
                'n' -> builder.append('\n')
                'r' -> builder.append('\r')
                't' -> builder.append('\t')
                'b' -> builder.append('\b')
                'f' -> builder.append("\\f")
                'u' -> {
                    val hex = StringBuilder()
                    if (i + 4 > length) {
                        throw RuntimeException("Not enough unicode digits!")
                    }
                    substring(i, i + 4).toCharArray().forEach { x ->
                        if (x.isLetterOrDigit().not()) throw RuntimeException("Bad character in unicode escape.")
                        hex.append(x.toLowerCase())
                    }
                    i += 4
                    val code = hex.toString().toInt(16)
                    builder.append(code.toChar())
                }
                else -> throw RuntimeException("Illegal escape sequence: \\$ch")
            }
        } else builder.append(delimiter)
    }
    return builder.toString()
}
