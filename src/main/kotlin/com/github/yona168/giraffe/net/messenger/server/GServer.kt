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
import com.github.yona168.giraffe.net.packet.Packet
import com.github.yona168.giraffe.net.packet.Size
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

class GServer(address: InetSocketAddress) : AbstractScopedPacketChannelComponent(), Server {
    private class ClientBufferPair(internal val client: Client, internal val buffer: ByteBuffer) {
        internal operator fun component1() = client
        internal operator fun component2() = buffer
    }

    private object AcceptHandler : ContinuationCompletionHandler<AsynchronousSocketChannel>

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
    private val channels: MutableMap<UUID, ClientBufferPair> = ConcurrentHashMap()
    override lateinit var socketChannel: AsynchronousServerSocketChannel
    override val clients: Set<Writable>
        get() = channels.values.map { it.client }.toSet()

    init {
        onEnable {
            socketChannel = AsynchronousServerSocketChannel.open()
            socketChannel.bind(address)
            launch {
                while (true) {
                    val clientChannel = accept()
                    val uuid = UUID.randomUUID()
                    val client = Client(clientChannel)
                    sendToClient(client, uuidPacket(uuid))
                    channels[uuid] = ClientBufferPair(client, ByteBuffer.allocate(maxByteLength))
                    launch readLaunch@{
                        while (true) {
                            val read = client.read()
                            if (read == -1) {
                                removeChannel(uuid)
                                return@readLaunch
                            }
                            val inbox = client.inbox
                            inbox.flip()
                            var opcode: Opcode
                            var size = Opcode.SIZE_BYTES + Size.SIZE_BYTES
                            while (size <= inbox.remaining()) {
                                opcode = inbox.getOpcode()
                                size = inbox.getSize()
                                if (inbox.remaining() < size) {
                                    inbox.compact()
                                } else {
                                    val packetBuffer=bufferPool.nextItem
                                    repeat(size){
                                        packetBuffer.put(inbox.get())
                                    }
                                    packetBuffer.flip()
                                    println("Server handling!!!")
                                    handlePacket(opcode,packetBuffer,client)
                                    bufferPool.clearAndRelease(packetBuffer)
                                }
                            }
                            inbox.flip()
                        }
                    }
                    yield()
                }
            }
        }
    }


    private suspend fun accept(): AsynchronousSocketChannel =
        suspendCancellableCoroutine { continuation ->
            socketChannel.accept(continuation, AcceptHandler)
        }

    override fun sendToClient(uuid: UUID, packet: Packet): Boolean {
        val channel = channels[uuid]?.client
        if (channel != null) {
            sendToClient(channel, packet)
            return true
        }
        return false
    }

    override fun sendToClient(writable: Writable, packet: Packet) {
        launch {
            writable.write(packet)
            yield()
        }

    }

    private fun removeChannel(uuid: UUID) {
        channels.remove(uuid)
    }


}





