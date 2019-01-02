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
    private val controller: IReadWriteController = ReadWriteController()
    private val writingPipeline = WritingPipeline()
private val handler= CoroutineExceptionHandler{exc,thing->thing.printStackTrace()}
    private inner class WritingPipeline {
        private val channel = Channel<Packet>(0)

        init {
            launch {
                while (true) {
                    val packet = channel.receive().build()
                    while (packet.hasRemaining()) {
                        println("Wrote!")
                        aWrite(packet)
                    }
                    yield()
                }

            }
        }

        fun send(packet: Packet) {
            launch {
                channel.send(packet)
            }
        }
    }

    init {
        onEnable {
            loopRead()
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

    override fun write(packet: Packet) = writingPipeline.send(packet)

    private suspend fun aWrite(buf: ByteBuffer): Int = suspendCancellableCoroutine {
        socketChannel.write(buf, it, WriteCompletionHandler)
    }

    internal fun loopRead() {
        launch {
            while (true) {
            controller.controlledAccess {
                val readResult = read()
                println(readResult)
                yield()
                if (readResult == -1) {
                    disable()
                    -1
                }
                var opcode: Opcode
                var size: Int = Opcode.SIZE_BYTES + Size.SIZE_BYTES
                inbox.flip()
                while (size <= inbox.remaining()) {
                    opcode = inbox.getOpcode()
                    println("Opcode: $opcode")
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
                    size=Opcode.SIZE_BYTES+Size.SIZE_BYTES
                }
                inbox.flip()
                1
            }
            }
        }
    }

}

object ReadCompletionHandler : ContinuationCompletionHandler<Int>
object WriteCompletionHandler : ContinuationCompletionHandler<Int>




