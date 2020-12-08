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