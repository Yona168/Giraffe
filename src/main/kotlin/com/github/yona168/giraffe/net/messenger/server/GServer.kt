package com.github.yona168.giraffe.net.messenger.server

import com.github.yona168.giraffe.net.ContinuationCompletionHandler
import com.github.yona168.giraffe.net.maxByteLength
import com.github.yona168.giraffe.net.messenger.AbstractScopedPacketChannelComponent
import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.messenger.client.Client
import com.github.yona168.giraffe.net.messenger.client.getOpcode
import com.github.yona168.giraffe.net.messenger.client.getSize
import com.github.yona168.giraffe.net.onEnable
import com.github.yona168.giraffe.net.packet.Opcode
import com.github.yona168.giraffe.net.packet.PacketBuilder
import com.github.yona168.giraffe.net.packet.Size
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.util.*
import kotlin.coroutines.CoroutineContext

class GServer(address: InetSocketAddress) : AbstractScopedPacketChannelComponent(), Server {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val channels: MutableMap<UUID, Client> = mutableMapOf()
    private val buffers=mutableMapOf<UUID,ByteBuffer>()
    override lateinit var socketChannel: AsynchronousServerSocketChannel
    override val clients: Set<Writable>
        get() = channels.values.toSet()

    init {
        onEnable {
            registerHandler(0) { packet, client ->
                val uuid = UUID(packet.readLong(), packet.readLong())
                channels[uuid] = (client as Client)
            }
            socketChannel = AsynchronousServerSocketChannel.open()
            socketChannel.bind(address)
            launch {
                while (true) {
                    accept()
                    yield()
                }
            }
            launch{
                loopRead()
            }
        }
    }

    private suspend fun loopRead() {
        while (true) {
            for (client in channels.values) {
                val buffer = buffers.computeIfAbsent(client.uuid){
                    ByteBuffer.allocate(maxByteLength)
                }
                val readResult = client.read(buffer)
                yield()
                if (readResult == -1) {
                    removeChannel(client.uuid)
                    return
                }
                var opcode: Opcode
                var size: Int = Opcode.SIZE_BYTES + Size.SIZE_BYTES
                while (size <= buffer.remaining()) {
                    opcode = buffer.getOpcode()
                    size = buffer.getSize()
                    if (buffer.remaining() < size) {
                        buffer.compact()
                    } else {
                        val packetBuffer = bufferPool.nextItem
                        repeat(size) {
                            packetBuffer.put(buffer.get())
                        }
                        packetBuffer.flip()
                        handlePacket(opcode, packetBuffer, client)
                        packetBuffer.clear()
                        bufferPool.release(packetBuffer)
                    }
                }
                buffer.flip()
            }

        }
    }

    private suspend fun accept(): AsynchronousSocketChannel =
        suspendCancellableCoroutine { continuation ->
            socketChannel.accept(continuation, AcceptHandler)
        }

    override fun sendToClient(uuid: UUID, packet: PacketBuilder): Boolean {
        val channel = channels[uuid]
        if (channel != null) {
            sendToClient(channel, packet)
            return true
        }
        return false
    }

    override fun sendToClient(writable: Writable, packet: PacketBuilder) {
        launch {
            writable.write(packet)
        }
    }

    private fun removeChannel(uuid:UUID){
        channels.remove(uuid)
        buffers.remove(uuid)
    }
    private object AcceptHandler : ContinuationCompletionHandler<AsynchronousSocketChannel>{
        override fun completed(
            result: AsynchronousSocketChannel,
            attachment: CancellableContinuation<AsynchronousSocketChannel>
        ) {
            super.completed(result, attachment)
        }
    }
}





