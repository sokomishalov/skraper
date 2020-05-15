package ru.sokomishalov.skraper.internal.nio

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import org.reactivestreams.Publisher
import java.io.File
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousCloseException
import java.nio.channels.AsynchronousFileChannel
import java.nio.channels.CompletionHandler
import java.nio.file.StandardOpenOption.*
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.resumeWithException

/**
 * @author sokomishalov
 */

internal suspend fun Publisher<ByteBuffer>.aWrite(file: File): Long {
    val mutex = Mutex()
    val position = AtomicLong(0)

    file.useChannel { afc ->
        asFlow().onEach {
            mutex.lock()
            suspendCancellableCoroutine<Int> { cont ->
                afc.write(it, position.get(), Triple(position, mutex, cont), PositionedCompletionHandler)
                cont.invokeOnCancellation { runCatching { afc.close() } }
            }
        }.collect()
    }

    return position.get()
}

internal inline fun <T> File.useChannel(block: (AsynchronousFileChannel) -> T): T {
    return AsynchronousFileChannel
            .open(toPath(), setOf(WRITE, TRUNCATE_EXISTING, CREATE), null)
            .use(block)
}

@Suppress("EXPERIMENTAL_API_USAGE")
private object PositionedCompletionHandler : CompletionHandler<Int, Triple<AtomicLong, Mutex, CancellableContinuation<Int>>> {
    override fun completed(bytesWritten: Int, attachment: Triple<AtomicLong, Mutex, CancellableContinuation<Int>>) {
        val (position, mutex, cont) = attachment
        position.addAndGet(bytesWritten.toLong())
        mutex.unlock()
        cont.resume(bytesWritten) {}
    }

    override fun failed(ex: Throwable, attachment: Triple<AtomicLong, Mutex, CancellableContinuation<Int>>) {
        val (position, mutex, cont) = attachment
        position.set(0)
        mutex.unlock()
        if (ex is AsynchronousCloseException && cont.isCancelled) return
        cont.resumeWithException(ex)
    }
}