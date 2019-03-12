package com.github.yona168.giraffe.net.messenger.server

import com.github.yona168.giraffe.net.ContinuationCompletionHandler
import com.github.yona168.giraffe.net.messenger.AbstractScopedPacketChannelComponent
import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.messenger.client.GClient
import com.github.yona168.giraffe.net.messenger.packetprocessor.ScopedPacketProcessor
import com.github.yona168.giraffe.net.onEnable
import com.github.yona168.giraffe.net.packet.SendablePacket
import com.github.yona168.giraffe.net.packet.uuidPacket
import kotlinx.coroutines.*
import java.net.SocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext
@ExperimentalCoroutinesApi
class GServer constructor(
    address: SocketAddress,
    packetProcessor: ScopedPacketProcessor
) : AbstractScopedPacketChannelComponent(packetProcessor), Server {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
    private val channels: MutableMap<UUID, GClient> = ConcurrentHashMap()
    override lateinit var socketChannel: AsynchronousServerSocketChannel
    override val clients: Collection<Writable>
        get() = channels.values

    private object AcceptHandler : ContinuationCompletionHandler<AsynchronousSocketChannel>()
    init {
        onEnable {
            socketChannel = AsynchronousServerSocketChannel.open().bind(address)
            launch(coroutineContext) {
                while (true) {
                    val clientChannel = accept()
                    val uuid = UUID.randomUUID()
                    val client = GClient(packetProcessor, clientChannel)
                    client.enable()
                    channels[uuid] = client
                    client.write(uuidPacket(uuid))
                    yield()
                }
            }
        }
    }


    private suspend fun accept(): AsynchronousSocketChannel =
        suspendCancellableCoroutine { continuation ->
            socketChannel.accept(continuation, AcceptHandler)
        }

    override fun sendToClient(uuid: UUID, packet: SendablePacket): Boolean {
        val channel = channels[uuid]
        if (channel != null) {
            channel.write(packet)
            return true
        }
        return false
    }


    private fun removeChannel(uuid: UUID) {
        channels.remove(uuid)
    }

    override suspend fun initShutdown() {
        socketChannel.close()
    }


}





