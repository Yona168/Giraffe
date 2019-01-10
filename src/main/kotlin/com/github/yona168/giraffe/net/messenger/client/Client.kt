package com.github.yona168.giraffe.net.messenger.client

import com.github.yona168.giraffe.net.ContinuationCompletionHandler
import com.github.yona168.giraffe.net.maxByteLength
import com.github.yona168.giraffe.net.messenger.AbstractScopedPacketChannelComponent
import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.onEnable
import com.github.yona168.giraffe.net.packet.Opcode
import com.github.yona168.giraffe.net.packet.Packet
import com.github.yona168.giraffe.net.packet.Size
import jdk.nashorn.internal.runtime.regexp.joni.constants.OPCode
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.net.InetSocketAddress
import java.nio.Buffer
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
    private val controller = Mutex()


    init {
        onEnable {
            launch{
                testLoopRead()
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

    internal suspend fun read(): Int{
        val read=read(inbox)
        if(read==-1){
            this@Client.disable()
            println("ERROR")
            return 0
        }
        else return read

    }

    internal suspend fun read(buf: ByteBuffer): Int = suspendCancellableCoroutine { cont ->
        socketChannel.read(buf, cont, ReadCompletionHandler)
    }

    override fun write(packet: Packet) {
        launch {
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
    //NOT WORKING YET
    internal suspend fun testLoopRead() {
        var opcode: Opcode? = null
        var size = opcodeAndSizeSize
        var currentRead = 0
        while(true) {
            if (currentRead < size) {
                while (currentRead < size) {
                    currentRead += read()
                }
            }
            inbox.flip()
            if(opcode==null){
                currentRead-=size
                opcode=inbox.getOpcode()
                size=inbox.getSize()
                inbox.unflip()
                continue
            }
            val buffer = bufferPool.nextItem
            val builder = StringBuilder()
            repeat(size) {
                val gotten = inbox.get()
                builder.append(gotten)
                buffer.put(gotten)
            }
            currentRead-=size
            println(builder.toString())
            buffer.flip()
            println("Buf size: ${buffer.buffer.remaining()}")
            println("Client is handling! Opcode=$opcode ReadSize=$size")
            handlePacket(
                opcode,
                buffer,
                this@Client
            )
            bufferPool.clearAndRelease(buffer)
            opcode = null
            size = opcodeAndSizeSize
            inbox.clearReadBytes()
            inbox.unflip()

        }



    }


}

object ReadCompletionHandler : ContinuationCompletionHandler<Int>
object WriteCompletionHandler : ContinuationCompletionHandler<Int>

private val opcodeAndSizeSize = Opcode.SIZE_BYTES + Size.SIZE_BYTES

private fun ByteBuffer.unflip() = limit(limit()+1).position(limit()).limit(capacity())
private fun ByteBuffer.clearReadBytes(): Buffer {
    val currentPosition=position()
    val currentLimit=limit()
    return compact().limit(currentLimit-currentPosition).position(0)
}