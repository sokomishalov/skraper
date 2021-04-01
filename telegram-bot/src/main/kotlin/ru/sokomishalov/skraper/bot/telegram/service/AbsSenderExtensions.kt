/*
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
package ru.sokomishalov.skraper.bot.telegram.service

import org.telegram.telegrambots.bots.DefaultAbsSender
import org.telegram.telegrambots.meta.api.methods.PartialBotApiMethod
import org.telegram.telegrambots.meta.api.methods.send.*
import java.io.Serializable

/**
 * @author sokomishalov
 */

internal fun <T : Serializable> DefaultAbsSender.send(method: PartialBotApiMethod<T>?) {
    when (method) {
        is SendMessage -> execute(method)
        is SendPhoto -> execute(method).also { method.photo.newMediaFile.delete() }
        is SendVideo -> execute(method).also { method.video.newMediaFile.delete() }
        is SendAudio -> execute(method).also { method.audio.newMediaFile.delete() }
        is SendDocument -> execute(method).also { method.document.newMediaFile.delete() }
        is SendMediaGroup -> execute(method)
        else -> Unit
    }
}