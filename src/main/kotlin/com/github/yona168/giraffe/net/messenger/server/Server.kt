package com.github.yona168.giraffe.net.messenger.server

import com.github.yona168.giraffe.net.messenger.Toggled
import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.messenger.client.Client
import com.github.yona168.giraffe.net.messenger.packetprocessor.CanProcessPackets
import com.github.yona168.giraffe.net.packet.SendablePacket
import kotlinx.coroutines.CoroutineScope
import java.util.*
import java.util.function.Consumer

/**
 * The interface used by [GServer]. In Giraffe, a Server is simply a router of [Client]s-it has no capabilities on its own.
 * As such, the functions defined here simply manage [Client]s
 */
interface Server : Toggled, CanProcessPackets, CoroutineScope {

    /**
     * A collection of all connected clients
     */
    val clients: Collection<Client>

    fun onConnect(func: Consumer<Client>): Boolean
    /**
     * Closes a client server-side. This calls [Client.close].
     * @param [uuid] The session [UUID] of the client, as referenced with [Client.sessionUUID]
     */
    fun closeClient(uuid: UUID)

    /**
     * Sends a packet to a specified client. This calls [Writable.write]
     * @param [uuid] The session [UUID] of the client, as referenced with [Client.sessionUUID]
     */
    fun sendToClient(uuid: UUID, packet: SendablePacket): Boolean

    /**
     * Sends a packet to all clients in [clients].
     * @param[packet] The [SendablePacket] to send
     */
    @JvmDefault
    fun sendToAllClients(packet: SendablePacket) = clients.forEach { it.write(packet) }

}