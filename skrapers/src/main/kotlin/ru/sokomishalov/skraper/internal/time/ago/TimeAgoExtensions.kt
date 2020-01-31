package ru.sokomishalov.skraper.internal.time.ago

import ru.sokomishalov.skraper.internal.time.ago.langs.EnglishTimeUnit
import java.time.Duration
import java.time.Period
import java.time.chrono.ChronoPeriod
import java.time.temporal.ChronoUnit.DAYS
import java.time.temporal.TemporalAmount

internal fun CharSequence.parseTimeAgo(timeUnit: IntlTimeUnit = EnglishTimeUnit): Long {
    val now = System.currentTimeMillis()

    val amount = split(" ")
            .firstOrNull()
            ?.toIntOrNull()
            ?: 1

    val temporalAmount: TemporalAmount = when {
        contains(timeUnit.moment, ignoreCase = true) or contains(timeUnit.moments, ignoreCase = true) -> Duration.ofMillis(amount.toLong())
        contains(timeUnit.second, ignoreCase = true) or contains(timeUnit.seconds, ignoreCase = true) -> Duration.ofSeconds(amount.toLong())
        contains(timeUnit.minute, ignoreCase = true) or contains(timeUnit.minutes, ignoreCase = true) -> Duration.ofMinutes(amount.toLong())
        contains(timeUnit.hour, ignoreCase = true) or contains(timeUnit.hours, ignoreCase = true) -> Duration.ofHours(amount.toLong())
        contains(timeUnit.day, ignoreCase = true) or contains(timeUnit.days, ignoreCase = true) -> Duration.ofDays(amount.toLong())
        contains(timeUnit.week, ignoreCase = true) or contains(timeUnit.weeks, ignoreCase = true) -> Period.ofWeeks(amount)
        contains(timeUnit.month, ignoreCase = true) or contains(timeUnit.months, ignoreCase = true) -> Period.ofMonths(amount)
        contains(timeUnit.year, ignoreCase = true) or contains(timeUnit.years, ignoreCase = true) -> Period.ofYears(amount)
        else -> Duration.ZERO
    }

    val millisAgo = when (temporalAmount) {
        is Duration -> temporalAmount.toMillis()
        is Period -> Duration.ofDays(temporalAmount.get(DAYS)).toMillis()
        is ChronoPeriod -> Duration.ofDays(temporalAmount.get(DAYS)).toMillis()
        else -> 0
    }

    return now - millisAgo
}