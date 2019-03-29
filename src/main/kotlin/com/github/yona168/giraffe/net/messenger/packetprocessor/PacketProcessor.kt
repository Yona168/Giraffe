package com.github.yona168.giraffe.net.messenger.packetprocessor

import com.github.yona168.giraffe.net.constants.Opcode
import com.github.yona168.giraffe.net.messenger.client.Client
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import com.gitlab.avelyn.architecture.base.Toggleable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

/**
 * A PacketProcessor is used to process [ReceivablePacket]s.
 */
interface PacketProcessor : Toggleable, CoroutineScope {
    /**
     * Registers a read [Opcode] to a function to process the packet.
     * @param[opcode] The [Opcode] to register.
     * @param[func] the function to register to the opcode.
     * @return This for chaining
     */
    fun on(opcode: Opcode, func: (Client, ReceivablePacket)->Unit): PacketProcessor

    /**
     * Unregisters a read [Opcode] from being processed.
     * @param[opcode] The opcode to stop listening to.
     * @return This for chaining
     */
    fun disableHandler(opcode: Opcode): PacketProcessor

    /**
     * Processes a [ReceivablePacket] by invoking a function, as registered with the passed [Opcode], with
     * the [ReceivablePacket] and an [Client]. This launches a new coroutine with [coroutineContext] context, and returns its [Job].
     * @param[opcode] the [Opcode] to get the registered function with.
     * @param[packet] The [ReceivablePacket] to handle.
     * @param[client] the [Client] to pass into the function.
     * @return[Job] of this coroutine
     */
    @JvmDefault
    fun handle(client: Client, opcode: Opcode, packet: ReceivablePacket):Job

    suspend fun handleSuspend(client: Client, opcode: Opcode, packet: ReceivablePacket){
        handle(client,opcode,packet).join()
    }
}