package com.github.yona168.giraffe.net

import kotlinx.coroutines.CancellableContinuation
import java.nio.ByteBuffer
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

const val MAX_PACKET_BYTE_SIZE=15000

typealias ByteBufferOp = (ByteBuffer).() -> Unit
typealias Opcode = Short
typealias Size = Int

internal open class ContinuationCompletionHandler<T>: CompletionHandler<T, CancellableContinuation<T>> {
    override fun completed(result: T, attachment: CancellableContinuation<T>) {
        attachment.resume(result)
    }

    override fun failed(exc: Throwable, attachment: CancellableContinuation<T>) {
        attachment.resumeWithException(exc)
    }

}