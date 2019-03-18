package com.github.yona168.giraffe.net.messenger.client

import com.github.yona168.giraffe.net.connect.SuspendCloseable
import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.messenger.packetprocessor.CanProcessPackets
import com.gitlab.avelyn.architecture.base.Toggleable
import kotlinx.coroutines.CoroutineScope
import java.util.*

/**
 * Defines the basic functionality of a client.
 *
 */
interface IClient : Writable, Toggleable, SuspendCloseable, CanProcessPackets, CoroutineScope {

    val sessionUUID: UUID?

    /**
     * Tells the client to execute [func] BEFORE it enables and connects, using this
     * [IClient] as a passed argument to that function.
     *
     * @param[func] the function to execute on connect
     * @return true if this process was successfully registered
     */
    fun preEnable(func: (IClient) -> Unit): Boolean

    /**
     * Tells the client to execute [func] prior to disconnecting from the server, using this
     * [IClient] as a passed argument to that function.
     *
     * @param[func] the function to execute on disconnect
     * @return true if this process was successfully registered
     */
    fun preDisconnect(func: (IClient) -> Unit): Boolean

    /**
     * Tells the client to execute [func] after disconnecting from the server, using
     * the closed [IClient] as a passed argument to that function
     *
     * @param[func] the function to execute post disconnect
     * @return true if this process was successfully registered
     */
    fun postDisconnect(func: (IClient) -> Unit): Boolean

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