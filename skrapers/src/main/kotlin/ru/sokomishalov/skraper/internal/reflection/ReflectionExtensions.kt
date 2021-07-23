package ru.sokomishalov.skraper.internal.reflection


internal fun classPathCheck(`class`: String): Boolean = runCatching { Class.forName(`class`) }.isSuccess