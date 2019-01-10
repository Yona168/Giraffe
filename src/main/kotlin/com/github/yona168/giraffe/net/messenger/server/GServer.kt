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
import javafx.application.Application.launch
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
    private val mutex = Mutex()

    init {
        onEnable {
            socketChannel = AsynchronousServerSocketChannel.open()
            socketChannel.bind(address)
            launch {
                while (true) {
                    val clientChannel = accept()
                    val uuid = UUID.randomUUID()
                    val client = Client(clientChannel)
                    println("UUID=${uuid.leastSignificantBits} and ${uuid.mostSignificantBits}")
                    sendToClient(client, uuidPacket(uuid))
                    channels[uuid] = ClientBufferPair(client, ByteBuffer.allocate(maxByteLength))
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





