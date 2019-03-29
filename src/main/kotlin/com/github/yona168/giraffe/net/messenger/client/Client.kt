package com.github.yona168.giraffe.net.messenger.client

import com.github.yona168.giraffe.net.constants.Opcode
import com.github.yona168.giraffe.net.messenger.Toggled
import com.github.yona168.giraffe.net.messenger.packetprocessor.CanProcessPackets
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import com.github.yona168.giraffe.net.packet.SendablePacket
import kotlinx.coroutines.CoroutineScope
import java.util.*
import java.util.concurrent.CompletableFuture

/**
 * Defines the basic functionality of a client. Giraffe implements this with [GClient].
 *
 */
interface Client : Toggled, CanProcessPackets, CoroutineScope {
    /**
     * The [UUID] given to the client by some server that it connects to. This allows for clients to easily target
     * other clients by referencing their session UUID in a [SendablePacket]. This is null if the server has not told
     * the client its UUID
     */
    val sessionUUID: UUID?

    /**
     * Sends a [SendablePacket] to the implementation
     *
     * @param packet The packet to send
     */
    fun write(packet: SendablePacket): CompletableFuture<Int>

    /**
     * Tells the client to execute this [func] when it receives a packet,
     * regardless of what packet that is. This [Client] is passed as an argument
     * to that function.
     *
     * @param[func] The function to execute when a packet is received, prior to processing it
     * @return this for chaining.
     */
    fun onPacketReceive(func: (Client) -> Unit): Client


    /**
     * Tells the client to execute this [func] when it receives a specified handshake packet from the server. This
     * means that packet sending across the socket channel is working, and thus this method is the earliest, safest method
     * for sending packets from.
     *
     * @param[func] the function to execute when a specified handshake packet is received.
     * @return this for chaining.
     */
    fun onHandshake(func: (Client) -> Unit): Client


    override fun onEnable(vararg listeners: Runnable): Client
    override fun onDisable(vararg listeners: Runnable): Client


    @JvmDefault
    override fun on(opcode: Opcode, func: (Client, ReceivablePacket) -> Unit): Client {
        super.on(opcode, func)
        return this
    }

    @JvmDefault
    override fun disableHandler(opcode: Opcode): Client {
        super.disableHandler(opcode)
        return this
    }


}