package com.github.yona168.giraffe.net

import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import kotlinx.coroutines.CancellableContinuation
import java.nio.ByteBuffer
import java.nio.channels.CompletionHandler
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

const val MAX_PACKET_BYTE_SIZE = 4096

typealias ByteBufferOp = (ByteBuffer).() -> Unit
typealias Opcode = Short
typealias Size = Int
typealias PacketHandlerFunction = (ReceivablePacket, Writable) -> Unit
internal open class ContinuationCompletionHandler<T>: CompletionHandler<T, CancellableContinuation<T>> {
    override fun completed(result: T, attachment: CancellableContinuation<T>) {
        attachment.resume(result)
    }

    override fun failed(exc: Throwable, attachment: CancellableContinuation<T>) {
        attachment.resumeWithException(exc)
    }

}

const val OPCODE_AND_SIZE_BYTE_SIZE: Int = Opcode.SIZE_BYTES + Size.SIZE_BYTES