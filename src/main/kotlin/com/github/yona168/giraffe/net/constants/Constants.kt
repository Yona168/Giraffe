@file:JvmName("Constants")

package com.github.yona168.giraffe.net.constants

import kotlinx.coroutines.CancellableContinuation
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException


const val MAX_PACKET_BYTE_SIZE = 4096

typealias Opcode = Byte
typealias Size = Int

/**
 * A [CompletionHandler] that resumes a [CancellableContinuation] on success, and resumes it with the exception on fail.
 */
open class ContinuationCompletionHandler<T> : CompletionHandler<T, CancellableContinuation<T>> {
    override fun completed(result: T, attachment: CancellableContinuation<T>) {
        attachment.resume(result)
    }

    override fun failed(exc: Throwable, attachment: CancellableContinuation<T>) {
        attachment.resumeWithException(exc)
    }

}

const val INTERNAL_OPCODE: Opcode = Opcode.MIN_VALUE

//Client bound
const val HANDSHAKE_SUB_IDENTIFIER: Opcode = 0
const val OPCODE_AND_SIZE_BYTE_SIZE: Int = Opcode.SIZE_BYTES + Size.SIZE_BYTES