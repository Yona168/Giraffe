package com.github.yona168.giraffe.net.messenger.client

import com.github.yona168.giraffe.net.*
import com.github.yona168.giraffe.net.messenger.AbstractScopedPacketChannelComponent
import com.github.yona168.giraffe.net.messenger.PacketHandler
import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.packet.Packet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AlreadyConnectedException
import java.nio.channels.AsynchronousSocketChannel
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

fun ByteBuffer.getOpcode() = short
fun ByteBuffer.getSize() = int

private const val OPCODE_AND_SIZE_BYTE_SIZE: Int = Opcode.SIZE_BYTES + Size.SIZE_BYTES

class Client constructor(
    override val socketChannel: AsynchronousSocketChannel = AsynchronousSocketChannel.open(),
    private val packetHandler: PacketHandler
) : AbstractScopedPacketChannelComponent(),
    Writable {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    internal val inbox = ByteBuffer.allocate(MAX_PACKET_BYTE_SIZE)
    private val controller = Mutex()


    init {
        onEnable {
            launch(coroutineContext) {
                loopRead()
            }
        }
        onDisable {
            packetHandler.disable()
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

    internal suspend fun read(): Int {
        val read = read(inbox)
        if (read == -1) {
            this@Client.disable()
            println("ERROR")
            return 0
        } else return read

    }

    internal suspend fun read(buf: ByteBuffer): Int = suspendCancellableCoroutine { cont ->
        socketChannel.read(buf, cont, ReadCompletionHandler)
    }

    override fun write(packet: Packet) {
        launch(coroutineContext) {
            controller.withLock {
                val buf = packet.build()
                while (buf.hasRemaining()) {
                    aWrite(buf)
                }
            }
        }
    }

    private suspend fun aWrite(buf: ByteBuffer): Int = suspendCancellableCoroutine {
        socketChannel.write(buf, it, WriteCompletionHandler)
    }

    private suspend fun loopRead() {
        var opcode: Opcode? = null
        var size = OPCODE_AND_SIZE_BYTE_SIZE
        var currentRead = 0
        while (true) {
            if (currentRead < size) {
                while (currentRead < size) {
                    currentRead += read()
                }
            }
            inbox.flip()
            if (opcode == null) {
                currentRead -= size
                opcode = inbox.getOpcode()
                size = inbox.getSize()
                inbox.compact()
                continue
            }
            val buffer = bufferPool.nextItem
            val builder = StringBuilder()
            repeat(size) {
                val gotten = inbox.get()
                builder.append(gotten)
                buffer.put(gotten)
            }
            currentRead -= size
            buffer.flip()
            println("Buf size: ${buffer.buffer.remaining()}")
            println("Client is handling! Opcode=$opcode ReadSize=$size")
            packetHandler.handlePacket(
                opcode,
                buffer,
                this@Client
            )
            bufferPool.clearAndRelease(buffer)
            opcode = null
            size = OPCODE_AND_SIZE_BYTE_SIZE
            inbox.compact()
            Thread.yield()
        }


    }


}

private object ReadCompletionHandler : ContinuationCompletionHandler<Int>()
private object WriteCompletionHandler : ContinuationCompletionHandler<Int>()
