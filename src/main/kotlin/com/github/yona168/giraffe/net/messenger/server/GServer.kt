package com.github.yona168.giraffe.net.messenger.server

import com.github.yona168.giraffe.net.ContinuationCompletionHandler
import com.github.yona168.giraffe.net.HANDSHAKE_SUB_IDENTIFIER
import com.github.yona168.giraffe.net.INTERNAL_OPCODE
import com.github.yona168.giraffe.net.messenger.AbstractScopedPacketChannelComponent
import com.github.yona168.giraffe.net.messenger.client.Client
import com.github.yona168.giraffe.net.messenger.client.GClient
import com.github.yona168.giraffe.net.messenger.packetprocessor.PacketProcessor
import com.github.yona168.giraffe.net.onEnable
import com.github.yona168.giraffe.net.packet.SendablePacket
import com.github.yona168.giraffe.net.packet.packetBuilder
import com.sun.security.ntlm.Server
import kotlinx.coroutines.*
import java.net.SocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import kotlin.coroutines.CoroutineContext

/**
 * The given implementation for [Server]. This class uses a [MutableMap] to store client [UUID]s to the serverside [Client] objects.
 * It overrides [CoroutineScope.coroutineContext] with [Dispatchers.IO]+[AbstractScopedPacketChannelComponent.job]. After
 * accepting a Client, it writes a handshake packet, giving it a session UUID as referenced by [Client.sessionUUID]. Any
 * [Client] that uses this server should handle the packet accordingly.
 */
class GServer constructor(
    address: SocketAddress,
    packetProcessor: PacketProcessor
) : AbstractScopedPacketChannelComponent(packetProcessor), com.github.yona168.giraffe.net.messenger.server.Server {

    private val onConnects = mutableSetOf<Consumer<Client>>()
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
    private val channels: MutableMap<UUID, Client> = ConcurrentHashMap()
    override lateinit var socketChannel: AsynchronousServerSocketChannel
    override val clients: Collection<Client>
        get() = channels.values

    companion object {
        //The [CompletionHandler]
        private object AcceptHandler : ContinuationCompletionHandler<AsynchronousSocketChannel>()

        private fun uuidPacket(uuid: UUID) = packetBuilder(INTERNAL_OPCODE, Consumer {
            it.writeByte(HANDSHAKE_SUB_IDENTIFIER)
            it.writeUUID(uuid)
        })
    }


    init {
        onEnable {
            socketChannel = AsynchronousServerSocketChannel.open().bind(address)
            launch(coroutineContext) {
                while (isActive) {
                    val clientChannel = accept()
                    clientChannel ?: continue
                    val uuid = UUID.randomUUID()
                    val client: Client = GClient(clientChannel, packetProcessor, uuid)
                    client.onDisable(Runnable { channels.remove(client.sessionUUID) })
                    channels[uuid] = client
                    client.enable()
                    client.write(uuidPacket(uuid))
                    onConnects.forEach { it.accept(client) }
                    yield()
                }
            }
        }
    }


    private suspend fun accept(): AsynchronousSocketChannel? =
        suspendCancellableCoroutine<AsynchronousSocketChannel> { cont ->
            socketChannel.accept(cont, AcceptHandler)
        }

    override fun onConnect(func: Consumer<Client>) = onConnects.add(func)

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

    override fun onEnable(vararg listeners: Runnable) = this.apply { super.onEnable(*listeners) }
    override fun onDisable(vararg listeners: Runnable) = this.apply { super.onDisable(*listeners) }


    override suspend fun initClose() {
        packetProcessor.disable()
        clients.forEach(::close)
    }

    private fun close(client: Client) {
        client.sessionUUID?.run(channels::remove)
        client.disable()
    }
}





