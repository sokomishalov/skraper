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
package ru.sokomishalov.skraper.internal.time.ago

import ru.sokomishalov.skraper.internal.time.ago.langs.EnglishTimeUnit
import java.time.Duration
import java.time.Period
import java.time.chrono.ChronoPeriod
import java.time.temporal.ChronoUnit.DAYS
import java.time.temporal.TemporalAmount

internal fun CharSequence.parseTimeAgo(lang: IntlTimeUnit = EnglishTimeUnit): Long {
    val now = System.currentTimeMillis()

    val amount = split(" ")
            .firstOrNull()
            ?.toIntOrNull()
            ?: 1

    val temporalAmount: TemporalAmount = when {
        contains(lang.moment, ignoreCase = true) or contains(lang.moments, ignoreCase = true) -> Duration.ofMillis(amount.toLong())
        contains(lang.second, ignoreCase = true) or contains(lang.seconds, ignoreCase = true) -> Duration.ofSeconds(amount.toLong())
        contains(lang.minute, ignoreCase = true) or contains(lang.minutes, ignoreCase = true) -> Duration.ofMinutes(amount.toLong())
        contains(lang.hour, ignoreCase = true) or contains(lang.hours, ignoreCase = true) -> Duration.ofHours(amount.toLong())
        contains(lang.day, ignoreCase = true) or contains(lang.days, ignoreCase = true) -> Duration.ofDays(amount.toLong())
        contains(lang.week, ignoreCase = true) or contains(lang.weeks, ignoreCase = true) -> Period.ofWeeks(amount)
        contains(lang.month, ignoreCase = true) or contains(lang.months, ignoreCase = true) -> Period.ofMonths(amount)
        contains(lang.year, ignoreCase = true) or contains(lang.years, ignoreCase = true) -> Period.ofYears(amount)
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