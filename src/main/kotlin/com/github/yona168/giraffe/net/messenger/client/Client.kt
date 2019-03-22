package com.github.yona168.giraffe.net.messenger.client

import com.github.yona168.giraffe.net.messenger.Toggled
import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.messenger.packetprocessor.CanProcessPackets
import com.github.yona168.giraffe.net.packet.SendablePacket
import kotlinx.coroutines.CoroutineScope
import java.util.*
import java.util.function.Consumer

/**
 * Defines the basic functionality of a client. Giraffe implements this with [GClient].
 *
 */
interface Client : Writable, Toggled, CanProcessPackets, CoroutineScope {

    /**
     * The [UUID] given to the client by some server that it connects to. This allows for clients to easily target
     * other clients by referencing their session UUID in a [SendablePacket]. This is null if the server has not told
     * the client its UUID
     */
    val sessionUUID: UUID?

    /**
     * Tells the client to execute this [func] when it receives a packet,
     * regardless of what packet that is. This [Client] is passed as an argument
     * to that function.
     *
     * @param[func] The function to execute when a packet is received
     *
     * @return true if this process was successfully registered
     */
    fun onPacketReceive(func: Consumer<Client>): Boolean

    fun onHandshake(func: Consumer<Client>): Boolean


}