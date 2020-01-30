package ru.sokomishalov.skraper.internal.url

internal fun String.uriCleanUp(): String = removePrefix("/")