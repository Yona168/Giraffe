package com.github.yona168.giraffe.net.messenger.server

import com.github.yona168.giraffe.net.messenger.Toggled
import com.github.yona168.giraffe.net.messenger.client.Client
import com.github.yona168.giraffe.net.messenger.packetprocessor.CanProcessPackets
import com.github.yona168.giraffe.net.packet.SendablePacket
import kotlinx.coroutines.CoroutineScope
import java.util.*
import java.util.function.Consumer

/**
 * The central object to route client connections.
 */
interface Server : Toggled, CanProcessPackets, CoroutineScope {

    /**
     * A collection of all connected clients
     */
    val clients: Collection<Client>

    /**
     * Registers a function to run with a connection once it connects.
     * @param[func] the function to execute, passing the connected client connection as an argument.
     * @return true if this process was successfully registered.
     */
    fun onConnect(func: Consumer<Client>): Boolean

    /**
     * Closes a client server-side. This disables the client on both ends.
     * @param [uuid] The session [UUID] of the client, as referenced with [Client.sessionUUID]
     */
    fun closeClient(uuid: UUID)

    /**
     * Sends a packet to a specified client. This calls [Client.write]
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