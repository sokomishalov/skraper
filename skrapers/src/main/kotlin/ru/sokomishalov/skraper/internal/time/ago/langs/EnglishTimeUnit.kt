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
package ru.sokomishalov.skraper.internal.time.ago.langs

import ru.sokomishalov.skraper.internal.time.ago.IntlTimeUnit

object EnglishTimeUnit : IntlTimeUnit {
    override val moment: String = "moment"
    override val moments: String = "moments"
    override val second: String = "second"
    override val seconds: String = "seconds"
    override val minute: String = "minute"
    override val minutes: String = "minute"
    override val hour: String = "hour"
    override val hours: String = "hour"
    override val day: String = "day"
    override val days: String = "day"
    override val week: String = "week"
    override val weeks: String = "week"
    override val month: String = "month"
    override val months: String = "month"
    override val year: String = "year"
    override val years: String = "year"
}