package com.github.yona168.giraffe.net.messenger.packetprocessor

import com.github.yona168.giraffe.net.constants.Opcode
import com.github.yona168.giraffe.net.messenger.client.Client
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import com.gitlab.avelyn.architecture.base.Toggleable
import kotlinx.coroutines.CoroutineScope

/**
 * A PacketProcessor is used to process [ReceivablePacket]s.
 */
interface PacketProcessor : Toggleable, CoroutineScope {
    /**
     * Registers a read [Opcode] to a [PacketHandlerFunction] to process the packet.
     * @param[opcode] The [Opcode] to register.
     * @param[func] the [PacketHandlerFunction] to register to the opcode.
     * @return The previously registered [PacketHandlerFunction] to the passed opcode, or null if no prior
     * function exists.
     */
    fun on(opcode: Opcode, func: PacketHandlerFunction): PacketHandlerFunction?

    /**
     * Unregisters a read [Opcode] from being processed.
     * @param[opcode] The opcode to stop listening to.
     * @return The [PacketHandlerFunction] that has been unregistered, or null if no function existed for [opcode].
     */
    fun disableHandler(opcode: Opcode): PacketHandlerFunction?

    /**
     * Processes a [ReceivablePacket] by invoking a [PacketHandlerFunction], as registered with the passed [Opcode], with
     * the [ReceivablePacket] and an [Client].
     * @param[opcode] the [Opcode] to get the registered [PacketHandlerFunction] with.
     * @param[packet] The [ReceivablePacket] to handle.
     * @param[client] the [Client] to pass into the [PacketHandlerFunction]
     */
    suspend fun handle(opcode: Opcode, packet: ReceivablePacket, client: Client)
}