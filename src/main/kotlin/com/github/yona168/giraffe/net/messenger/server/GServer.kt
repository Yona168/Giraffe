package com.github.yona168.giraffe.net.messenger.server

import com.github.yona168.giraffe.net.constants.ContinuationCompletionHandler
import com.github.yona168.giraffe.net.constants.HANDSHAKE_SUB_IDENTIFIER
import com.github.yona168.giraffe.net.constants.INTERNAL_OPCODE
import com.github.yona168.giraffe.net.messenger.AbstractScopedPacketChannelComponent
import com.github.yona168.giraffe.net.messenger.client.Client
import com.github.yona168.giraffe.net.messenger.client.GClient
import com.github.yona168.giraffe.net.messenger.packetprocessor.PacketProcessor
import com.github.yona168.giraffe.net.onEnable
import com.github.yona168.giraffe.net.packet.SendablePacket
import com.github.yona168.giraffe.net.packet.packetBuilder
import kotlinx.coroutines.*
import java.net.SocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.function.Consumer
import kotlin.coroutines.CoroutineContext

/**
 * The given implementation for [Server]. This class uses a [MutableMap] to store client [UUID]s to the [Client] objects, which represent
 * server-side connections.
 *[CoroutineScope.coroutineContext] is overriden with [Dispatchers.IO]+[AbstractScopedPacketChannelComponent.job]. After
 * accepting a Client, it writes a handshake packet (see [GClient] description), giving it a session UUID as referenced by [Client.sessionUUID]. Any
 * [Client] that uses this server should handle the packet accordingly, or ignore it.
 *
 * @param[address] The [SocketAddress] to have the server run on
 * @param[packetProcessor] The [PacketProcessor] to process received packets with.
 */
class GServer constructor(
    address: SocketAddress,
    packetProcessor: PacketProcessor
) : AbstractScopedPacketChannelComponent(packetProcessor), Server {
    /**
     * The [CoroutineContext] that this client will use for launching coroutines. In [GServer],
     * this is set to [Dispatchers.IO]+[job]
     */
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    /**
     * The [AsynchronousServerSocketChannel] that accepts client connections
     */
    override val socketChannel: AsynchronousServerSocketChannel
        get() = backingSocketChannel

    private lateinit var backingSocketChannel: AsynchronousServerSocketChannel

    override val clients: Collection<Client>
        get() = channels.values
    private val onConnects = mutableSetOf<(Client)->Unit>()
    private val channels: MutableMap<UUID, Client> = ConcurrentHashMap()

    init {
        onEnable {
            backingSocketChannel = AsynchronousServerSocketChannel.open().bind(address)
            launch(coroutineContext) {
                while (isActive) {
                    val clientChannel = accept()
                    clientChannel ?: continue
                    val uuid = UUID.randomUUID()
                    val client: Client = GClient.newServerside(clientChannel, packetProcessor, uuid)
                    client.onDisable(Runnable { channels.remove(client.sessionUUID) })
                    channels[uuid] = client
                    client.enable()
                    client.write(uuidPacket(uuid))
                    onConnects.forEach { it(client) }
                    yield()
                }
            }
        }
    }


    private suspend fun accept(): AsynchronousSocketChannel? =
        suspendCancellableCoroutine<AsynchronousSocketChannel> { cont ->
            socketChannel.accept(cont, AcceptHandler)
        }

    override fun onConnect(func: (Client)->Unit) = apply{onConnects.add(func)}

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

    override fun onEnable(vararg listeners: java.lang.Runnable)=apply{super<AbstractScopedPacketChannelComponent>.onEnable(*listeners)}
    override fun onEnable(listeners: ()->Unit)=apply { super<Server>.onEnable(listeners) }

    override fun onDisable(vararg listeners: java.lang.Runnable)=apply{super<AbstractScopedPacketChannelComponent>.onDisable(*listeners)}
    override fun onDisable(listeners: ()->Unit)=apply { super<Server>.onDisable(listeners) }


    override suspend fun initClose() {
        packetProcessor.disable()
        clients.forEach(::close)
    }

    private fun close(client: Client) {
        client.sessionUUID?.run(channels::remove)
        client.disable()
    }

    companion object {
        /**
         * The [ContinuationCompletionHandler] to use to accept channels
         */
        private object AcceptHandler : ContinuationCompletionHandler<AsynchronousSocketChannel>()

        /**
         * Supplier for handshake packets
         */
        private fun uuidPacket(uuid: UUID) = packetBuilder(INTERNAL_OPCODE){
            it.writeByte(HANDSHAKE_SUB_IDENTIFIER)
            it.writeUUID(uuid)
        }
    }
}





