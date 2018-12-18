package com.github.yona168.giraffe.net

import com.github.yona168.giraffe.net.messenger.Networker
import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.packet.Opcode
import com.github.yona168.giraffe.net.packet.PacketBuilder
import com.github.yona168.giraffe.net.packet.Size
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.CharBuffer
import java.nio.channels.AlreadyConnectedException
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

fun ByteBuffer.getOpcode() = short
fun ByteBuffer.getSize() = int


class Client : Networker(), Writable {

    override lateinit var socketChannel: AsynchronousSocketChannel
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
    private val inbox = ByteBuffer.allocate(maxByteLength)

    init {
        onEnable {
            socketChannel = AsynchronousSocketChannel.open()
            launch {
                readLooper()
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
                    socketChannel.close()
                }
            }
        }
    }

    private suspend fun read() = suspendCancellableCoroutine<Int> {
        socketChannel.read(inbox, it, ReadCompletionHandler)
    }

    override suspend fun write(builder: PacketBuilder) = suspendCancellableCoroutine<Int> {
        socketChannel.write(inbox, it, WriteCompletionHandler)
    }

    private suspend fun readLooper() {
        while (true) {
            val readResult = read()
            yield()
            if (readResult == -1) {
                disable()
            }
            var opcode: Opcode
            var size: Int = Opcode.SIZE_BYTES + Size.SIZE_BYTES
            while (size <= inbox.remaining()) {
                opcode = inbox.getOpcode()
                size = inbox.getSize()
                if (inbox.remaining() < size) {
                    inbox.compact()
                } else {
                    val buffer = bufferPool.buffer
                    repeat(size) {
                        buffer.put(inbox.get())
                    }
                    buffer.flip()
                    withContext(Dispatchers.Main) {
                        handlePacket(opcode, buffer)
                        bufferPool.release(buffer)
                    }
                }
            }
        }
    }

}

object ReadCompletionHandler : ContinuationCompletionHandler<Int>
object WriteCompletionHandler : ContinuationCompletionHandler<Int>






