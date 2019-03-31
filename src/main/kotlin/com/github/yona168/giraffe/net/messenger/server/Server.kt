package com.github.yona168.giraffe.net.messenger.server

import com.github.yona168.giraffe.net.constants.Opcode
import com.github.yona168.giraffe.net.messenger.Toggled
import com.github.yona168.giraffe.net.messenger.client.Client
import com.github.yona168.giraffe.net.messenger.packetprocessor.CanProcessPackets
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import com.github.yona168.giraffe.net.packet.SendablePacket
import kotlinx.coroutines.CoroutineScope
import java.util.*

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
     * @return this for chaining
     */
    fun onConnect(func: (Client) -> Unit): Server

    /**
     * Closes a client server-side. This disables the client on both ends.
     * @param [uuid] The session [UUID] of the client, as referenced with [Client.sessionUUID]
     */
    fun closeClient(uuid: UUID)

    /**
     * Sends a packet to a specified client. This calls [Client.write]
     * @param [uuid] The session [UUID] of the client, as referenced with [Client.sessionUUID]
     * @return true if a client for [uuid] existed and was written [packet]
     */
    fun sendToClient(uuid: UUID, packet: SendablePacket): Boolean

    /**
     * Sends a packet to all clients in [clients].
     * @param[packet] The [SendablePacket] to send
     */
    @JvmDefault
    fun sendToAllClients(packet: SendablePacket) = clients.forEach { it.write(packet) }

    override fun onEnable(vararg listeners: Runnable): Server
    @JvmDefault
    override fun onEnable(function: () -> Unit)=apply{super.onEnable(function)}

    override fun onDisable(vararg listeners: Runnable): Server
    @JvmDefault
    override fun onDisable(function: () -> Unit)=apply{super.onDisable(function)}


    @JvmDefault
    override fun on(opcode: Opcode, func: (Client, ReceivablePacket) -> Unit): Server {
        super.on(opcode, func)
        return this
    }

    @JvmDefault
    override fun disableHandler(opcode: Opcode): Server {
        super.disableHandler(opcode)
        return this
    }

}