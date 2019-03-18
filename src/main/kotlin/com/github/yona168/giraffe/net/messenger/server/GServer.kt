package com.github.yona168.giraffe.net.messenger.server

import com.github.yona168.giraffe.net.ContinuationCompletionHandler
import com.github.yona168.giraffe.net.HANDSHAKE_SUB_IDENTIFIER
import com.github.yona168.giraffe.net.INTERNAL_OPCODE
import com.github.yona168.giraffe.net.messenger.AbstractScopedPacketChannelComponent
import com.github.yona168.giraffe.net.messenger.client.Client
import com.github.yona168.giraffe.net.messenger.client.GClient
import com.github.yona168.giraffe.net.messenger.client.IClient
import com.github.yona168.giraffe.net.messenger.packetprocessor.ScopedPacketProcessor
import com.github.yona168.giraffe.net.onEnable
import com.github.yona168.giraffe.net.packet.SendablePacket
import com.github.yona168.giraffe.net.packet.packetBuilder
import kotlinx.coroutines.*
import java.net.SocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

/**
 * The given implementation for [Server]. This class uses a [MutableMap] to store client [UUID]s to their session [UUID]'s.
 * It overrides [CoroutineScope.coroutineContext] with [Dispatchers.IO]+[AbstractScopedPacketChannelComponent.job]. After
 * accepting a Client, it writes a handshake packet, giving it a session UUID as referenced by [IClient.sessionUUID]. Any
 * [Client] that uses this server should handle the packet accordingly.
 */
class GServer constructor(
    address: SocketAddress,
    override val packetProcessor: ScopedPacketProcessor
) : AbstractScopedPacketChannelComponent(packetProcessor), IServer {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
    private val channels: MutableMap<UUID, IClient> = ConcurrentHashMap()
    override lateinit var socketChannel: AsynchronousServerSocketChannel
    override val clients: Collection<IClient>
        get() = channels.values

    companion object {
        private fun uuidPacket(uuid: UUID) = packetBuilder(INTERNAL_OPCODE) {
            writeByte(HANDSHAKE_SUB_IDENTIFIER)
            writeUUID(uuid)
        }

        private object AcceptHandler : ContinuationCompletionHandler<AsynchronousSocketChannel>()
    }


    init {
        addChild(packetProcessor)
        onEnable {
            socketChannel = AsynchronousServerSocketChannel.open().bind(address)
            launch(coroutineContext) {
                while (isActive) {
                    val clientChannel = accept()
                    clientChannel ?: continue
                    val uuid = UUID.randomUUID()
                    val client: IClient =
                        GClient(clientChannel, packetProcessor, uuid) { client -> channels.remove(client.sessionUUID) }
                    channels[uuid] = client
                    client.enable()
                    client.write(uuidPacket(uuid))
                    yield()
                }
            }
        }
    }


    private suspend fun accept(): AsynchronousSocketChannel? =
        suspendCancellableCoroutine<AsynchronousSocketChannel> { cont ->
            socketChannel.accept(cont, AcceptHandler)
        }


    override fun closeClient(uuid: UUID) {
        channels[uuid]?.apply { close(this) }
    }

    override fun sendToClient(uuid: UUID, packet: SendablePacket): Boolean {
        val channel = channels[uuid]
        if (channel != null) {
            channel.write(packet)
            return true
        }
        return false
    }


    override suspend fun close() {
        clients.forEach(::close)
        socketChannel.close()
        cancelCoroutines()
    }

    private fun close(client: IClient) {
        client.sessionUUID?.run(channels::remove)
        client.disable()
    }
}





