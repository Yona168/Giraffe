package com.github.yona168.giraffe.net

import com.github.yona168.giraffe.net.messenger.Networker
import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.packet.Opcode
import com.github.yona168.giraffe.net.packet.PacketBuilder
import com.github.yona168.giraffe.net.packet.Size
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AlreadyConnectedException
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

fun ByteBuffer.getOpcode() = short
fun ByteBuffer.getSize() = int


class Client(override val socketChannel: AsynchronousSocketChannel = AsynchronousSocketChannel.open()) : Networker(),
    Writable {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
    private val inbox = ByteBuffer.allocate(maxByteLength)
    init {
        onEnable {
            runBlocking {
                loopRead()
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

    private suspend fun read(): Int = suspendCancellableCoroutine { cont ->
        socketChannel.read(inbox, cont, ReadCompletionHandler)
    }

    override fun write(builder: PacketBuilder){
        launch{
            val buf=builder.build()
            while(buf.hasRemaining()){
                aWrite(buf)
            }
        }
    }

   private suspend fun aWrite(buf:ByteBuffer): Int = suspendCancellableCoroutine {
            socketChannel.write(buf, it, WriteCompletionHandler)
    }

    private suspend fun loopRead() {
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
                        val buffer = bufferPool.buffer
                        repeat(size) {
                            buffer.put(inbox.get())
                        }
                        buffer.flip()
                            handlePacket(opcode, buffer)
                            bufferPool.release(buffer)

                    }
                }
                inbox.flip()
            }
        }
    }

}

object ReadCompletionHandler : ContinuationCompletionHandler<Int>
object WriteCompletionHandler : ContinuationCompletionHandler<Int>






