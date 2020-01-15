@file:Suppress("NOTHING_TO_INLINE")

package ru.sokomishalov.skraper.internal.util.time

import java.time.Duration
import java.util.*

/**
 * @author sokomishalov
 */

@PublishedApi
internal inline fun mockDate(
        itemIndex: Int = 0,
        from: Date = Date(),
        minusMultiply: Duration = Duration.ofMinutes(10)
): Date {
    return Date.from(from.toInstant() - (minusMultiply.multipliedBy(itemIndex.toLong())))
}