package com.github.yona168.giraffe.net.messenger.client

import com.github.yona168.giraffe.net.ContinuationCompletionHandler
import com.github.yona168.giraffe.net.maxByteLength
import com.github.yona168.giraffe.net.messenger.AbstractScopedPacketChannelComponent
import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.onEnable
import com.github.yona168.giraffe.net.packet.Opcode
import com.github.yona168.giraffe.net.packet.Packet
import com.github.yona168.giraffe.net.packet.Size
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AlreadyConnectedException
import java.nio.channels.AsynchronousSocketChannel
import java.util.*

import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

fun ByteBuffer.getOpcode() = short
fun ByteBuffer.getSize() = int


class Client constructor(
    override val socketChannel: AsynchronousSocketChannel = AsynchronousSocketChannel.open()
) : AbstractScopedPacketChannelComponent(),
    Writable {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
    internal val inbox = ByteBuffer.allocate(maxByteLength)
    private val writingPipeline = Channel<Packet>(0)

    init {
        onEnable {
            loopRead()
            launch {
                while (true) {
                    val packet = writingPipeline.receive().build()
                    while (packet.hasRemaining()) {
                        aWrite(packet)
                    }
                    yield()
                }
            }
        }
    }

    fun connectTo(
        address: InetSocketAddress,
        unit: TimeUnit,
        timeout: Long,
        onTimeoutFunction: (Client) -> Unit
    ) {
        val connectionResult = runCatching<Client> {
            socketChannel.connect(address).get(timeout, unit)
            this
        }
        connectionResult.onFailure { exc ->
            print("Cause: ${exc.cause} Message: ${exc.message}")
            when (exc) {
                is AlreadyConnectedException -> {
                    error("Can't connect when Client is already connected!")
                }
                else -> {
                    onTimeoutFunction(this)
                }
            }
        }
    }

    internal suspend fun read(): Int = read(inbox)
    internal suspend fun read(buf: ByteBuffer): Int = suspendCancellableCoroutine { cont ->
        socketChannel.read(buf, cont, ReadCompletionHandler)
    }

    override fun write(packet: Packet) {
        launch {
            writingPipeline.send(packet)
        }
    }

    private suspend fun aWrite(buf: ByteBuffer): Int = suspendCancellableCoroutine {
        socketChannel.write(buf, it, WriteCompletionHandler)
    }

    internal fun loopRead() {
        launch {
            while (true) {
                val readResult = read()
                yield()
                if (readResult == -1) {
                    disable()
                    return@launch
                }
                var opcode: Opcode
                var size: Int = Opcode.SIZE_BYTES + Size.SIZE_BYTES
                inbox.flip()
                while (size <= inbox.remaining()) {
                    opcode = inbox.getOpcode()
                    size = inbox.getSize()
                    if (inbox.remaining() < size) {
                        inbox.compact()
                    } else {
                        val buffer = bufferPool.nextItem
                        repeat(size) {
                            buffer.put(inbox.get())
                        }
                        buffer.flip()
                        println("Client is handling!!!!")
                        handlePacket(opcode, buffer, this@Client)
                        bufferPool.clearAndRelease(buffer)
                    }
                }
                inbox.flip()
            }
        }
    }

}

object ReadCompletionHandler : ContinuationCompletionHandler<Int>
object WriteCompletionHandler : ContinuationCompletionHandler<Int> {
    override fun completed(result: Int, attachment: CancellableContinuation<Int>) {
        super.completed(result, attachment)
    }
}




