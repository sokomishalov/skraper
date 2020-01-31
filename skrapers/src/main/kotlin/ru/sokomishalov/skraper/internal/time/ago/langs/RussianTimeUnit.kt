package ru.sokomishalov.skraper.internal.time.ago.langs

import ru.sokomishalov.skraper.internal.time.ago.IntlTimeUnit

object RussianTimeUnit : IntlTimeUnit {
    override val moment: String = "момент"
    override val moments: String = "моментов"
    override val second: String = "секунда"
    override val seconds: String = "секунд"
    override val minute: String = "минут"
    override val minutes: String = "минут"
    override val hour: String = "час"
    override val hours: String = "часов"
    override val day: String = "день"
    override val days: String = "дней"
    override val week: String = "недел"
    override val weeks: String = "недель"
    override val month: String = "месяц"
    override val months: String = "месяцев"
    override val year: String = "год"
    override val years: String = "лет"
}