package com.github.yona168.giraffe.net.messenger.server

import com.github.yona168.giraffe.net.ContinuationCompletionHandler
import com.github.yona168.giraffe.net.messenger.AbstractScopedPacketChannelComponent
import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.messenger.client.Client
import com.github.yona168.giraffe.net.messenger.packetprocessor.CoroutineDispatcherPacketProcessor
import com.github.yona168.giraffe.net.messenger.packetprocessor.PacketProcessor
import com.github.yona168.giraffe.net.onEnable
import com.github.yona168.giraffe.net.packet.Packet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

class GServer @JvmOverloads constructor(
    address: InetSocketAddress,
    packetProcessor: PacketProcessor = CoroutineDispatcherPacketProcessor(
        Dispatchers.Default
    )
) : AbstractScopedPacketChannelComponent(packetProcessor), Server {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
    private val channels: MutableMap<UUID, Client> = ConcurrentHashMap()
    override lateinit var socketChannel: AsynchronousServerSocketChannel
    override val clients: Collection<Writable>
        get() = channels.values

    private object AcceptHandler : ContinuationCompletionHandler<AsynchronousSocketChannel>()
    init {
        onEnable {
            socketChannel = AsynchronousServerSocketChannel.open()
            socketChannel.bind(address)
            launch(coroutineContext) {
                while (true) {
                    val clientChannel = accept()
                    val uuid = UUID.fromString("f5e9d8e2-18ec-4330-bdbc-2e41e9bb363f")
                    val client = Client(packetProcessor, clientChannel)
                    println("UUID=${uuid.leastSignificantBits} and ${uuid.mostSignificantBits}")
                    sendToClient(client, uuidPacket(uuid))
                    channels[uuid] = client
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
        val channel = channels[uuid]
        if (channel != null) {
            sendToClient(channel, packet)
            return true
        }
        return false
    }

    override fun sendToClient(writable: Writable, packet: Packet) {
        writable.write(packet)

    }

    private fun removeChannel(uuid: UUID) {
        channels.remove(uuid)
    }


}





