package com.github.yona168.giraffe.net

import kotlinx.coroutines.*
import java.io.IOException
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.Channel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.*

suspend fun Client.aWrite(src: ByteBuffer): Int = suspendCancellableCoroutine {
    this.write(src, Unit, object : CompletionHandler<Int, Unit> {
        override fun completed(result: Int, attachment: Unit) {
         //TODO
        }
        override fun failed(exc: Throwable, attachment: Unit?) {
            this@aWrite.closeOnCancelOf(it)
            it.resumeWithException(exc)
        }

    })
}

fun Channel.closeOnCancelOf(cont: CancellableContinuation<*>) {
    cont.invokeOnCancellation {
        try {
            this.close()
        } catch (exc: IOException) {

        }
    }
}