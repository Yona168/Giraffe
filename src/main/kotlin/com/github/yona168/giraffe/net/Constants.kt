package com.github.yona168.giraffe.net

import com.github.yona168.giraffe.net.messenger.client.Client
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import kotlinx.coroutines.CancellableContinuation
import java.nio.ByteBuffer
import java.nio.channels.CompletionHandler
import java.util.function.BiConsumer
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

const val MAX_PACKET_BYTE_SIZE = 4096

typealias ByteBufferOp = (ByteBuffer).() -> Unit
typealias Opcode = Byte
typealias Size = Int
typealias PacketHandlerFunction = BiConsumer<ReceivablePacket, Client>
internal open class ContinuationCompletionHandler<T>: CompletionHandler<T, CancellableContinuation<T>> {
    override fun completed(result: T, attachment: CancellableContinuation<T>) {
        attachment.resume(result)
    }

    override fun failed(exc: Throwable, attachment: CancellableContinuation<T>) {
        attachment.resumeWithException(exc)
    }

}
const val INTERNAL_OPCODE: Opcode = -1

//Client bound
const val HANDSHAKE_SUB_IDENTIFIER: Opcode = 0
const val OPCODE_AND_SIZE_BYTE_SIZE: Int = Opcode.SIZE_BYTES + Size.SIZE_BYTES