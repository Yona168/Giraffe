package com.github.yona168.giraffe.net.messenger.client

import com.github.yona168.giraffe.net.messenger.Writable
import com.gitlab.avelyn.architecture.base.Toggleable
import java.util.*

/**
 * Defines the basic functionality of a client.
 *
 */
interface Client : Writable, Toggleable {

    val sessionUUID: UUID?

    /**
     * Tells the client to execute [func] when it connects to a server, using this
     * [Client] as a passed argument to that function.
     *
     * @param[func] the function to execute on connect
     * @return true if this process was successfully registered
     */
    fun onConnect(func: (Client) -> Unit): Boolean

    /**
     * Tells the client to execute [func] when it connects to a server.
     *
     * @param[func] the function to execute on connect
     * @return true if this process was successfully registered
     */
    fun onConnect(func: () -> Unit): Boolean

    /**
     * Tells the client to execute [func] when it disconnects from a server, using this
     * [Client] as a passed argument to that function.
     *
     * @param[func] the function to execute on disconnect
     * @return true if this process was successfully registered
     */
    fun onDisconnect(func: (Client) -> Unit): Boolean

    /**
     * Tells the client to execute [func] when it disconnects from a server.
     *
     * @param[func] the function to execute on disconnect
     * @return true if this process was successfully registered
     */
    fun onDisconnect(func: () -> Unit): Boolean


    /**
     * Tells the client to execute this [func] when it receives a packet,
     * regardless of what packet that is. This [Client] is passed as an argument
     * to that function.
     *
     * @param[func] The function to execute when a packet is received
     *
     * @return true if this process was successfully registered
     */
    fun onPacketReceive(func: (Client) -> Unit): Boolean

    /**
     * Tells the client to execute this [func] when it receives a packet,
     * regardless of what packet that is.
     *
     * @param[func] The function to execute when a packet is received
     *
     * @return true if this process was successfully registered
     */
    fun onPacketReceive(func: () -> Unit): Boolean

}