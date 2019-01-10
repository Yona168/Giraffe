package com.github.yona168.giraffe.net.messenger.server

import com.github.yona168.giraffe.net.ContinuationCompletionHandler
import com.github.yona168.giraffe.net.MAX_PACKET_BYTE_SIZE
import com.github.yona168.giraffe.net.messenger.AbstractScopedPacketChannelComponent
import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.messenger.client.Client
import com.github.yona168.giraffe.net.onEnable
import com.github.yona168.giraffe.net.packet.Packet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.yield
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.SocketChannel
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

class GServer(address: InetSocketAddress) : AbstractScopedPacketChannelComponent(), Server {
    private class ClientBufferPair(internal val client: Client, internal val buffer: ByteBuffer) {
        internal operator fun component1() = client
        internal operator fun component2() = buffer
    }


    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
    private val channels: MutableMap<UUID, ClientBufferPair> = ConcurrentHashMap()
    override lateinit var socketChannel: AsynchronousServerSocketChannel
    override val clients: Set<Writable>
        get() = channels.values.map { it.client }.toSet()
    private val mutex = Mutex()

    init {
        onEnable {
            socketChannel = AsynchronousServerSocketChannel.open()
            socketChannel.bind(address)
            launch(coroutineContext) {
                while (true) {
                    val clientChannel = accept()
                    val uuid = UUID.randomUUID()
                    val client = Client(clientChannel)
                    println("UUID=${uuid.leastSignificantBits} and ${uuid.mostSignificantBits}")
                    sendToClient(client, uuidPacket(uuid))
                    channels[uuid] = ClientBufferPair(client, ByteBuffer.allocate(MAX_PACKET_BYTE_SIZE))
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
        launch(coroutineContext) {
            writable.write(packet)
            yield()
        }

    }

    private fun removeChannel(uuid: UUID) {
        channels.remove(uuid)
    }


}



private object AcceptHandler:ContinuationCompletionHandler<AsynchronousSocketChannel>()

