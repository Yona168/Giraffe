package com.github.yona168.giraffe.net.messenger.client

import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.messenger.packetprocessor.CanProcessPackets
import com.github.yona168.giraffe.net.packet.SendablePacket
import com.gitlab.avelyn.architecture.base.Toggleable
import kotlinx.coroutines.CoroutineScope
import java.util.*

/**
 * Defines the basic functionality of a client. Giraffe implements this with [GClient].
 *
 */
interface IClient : Writable, Toggleable, CanProcessPackets, CoroutineScope {

    /**
     * The [UUID] given to the client by some server that it connects to. This allows for clients to easily target
     * other clients by referencing their session UUID in a [SendablePacket]. This is null if the server has not told
     * the client its UUID
     */
    val sessionUUID: UUID?

    /**
     * Tells the client to execute this [func] when it receives a packet,
     * regardless of what packet that is. This [IClient] is passed as an argument
     * to that function.
     *
     * @param[func] The function to execute when a packet is received
     *
     * @return true if this process was successfully registered
     */
    fun onPacketReceive(func: (IClient) -> Unit): Boolean

    fun onHandshake(func: (IClient) -> Unit): Boolean


}