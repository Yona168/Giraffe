package com.github.yona168.giraffe.net

import kotlinx.coroutines.CancellableContinuation
import java.nio.channels.Channel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun Channel.closeOnCancelOf(o: CancellableContinuation<*>){
    o.invokeOnCancellation { close() }
}

interface ContinuationCompletionHandler<T>: CompletionHandler<T, CancellableContinuation<T>> {
    override fun failed(exc: Throwable, attachment: CancellableContinuation<T>) = attachment.resumeWithException(exc)
    override fun completed(result: T, attachment: CancellableContinuation<T>) = attachment.resume(result)
}