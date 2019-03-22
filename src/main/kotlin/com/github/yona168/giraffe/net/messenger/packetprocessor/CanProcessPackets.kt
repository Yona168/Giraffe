package com.github.yona168.giraffe.net.messenger.packetprocessor

import com.github.yona168.giraffe.net.Opcode
import com.github.yona168.giraffe.net.PacketHandlerFunction
import com.github.yona168.giraffe.net.messenger.client.Client
import com.github.yona168.giraffe.net.packet.ReceivablePacket

/**
 * Defines a class that has a [PacketProcessor] as a field
 * @property[packetProcessor] The [PacketProcessor] contained by the implementing class
 * @see[PacketProcessor]
 */
interface CanProcessPackets {
    val packetProcessor: PacketProcessor
    /**
     * Calls [PacketProcessor.handle] with [packetProcessor]
     * @see[PacketProcessor.handle]
     */
    suspend fun handle(opcode: Opcode, packet: ReceivablePacket, networker: Client) =
        packetProcessor.handle(opcode, packet, networker)

    /**
     * Calls [PacketProcessor.on] with [packetProcessor]
     * @see[PacketProcessor.on]
     */
    fun on(opcode: Opcode, func: PacketHandlerFunction) = packetProcessor.on(opcode, func)

    /**
     * Calls [PacketProcessor.disableHandler] with [packetProcessor]
     * @see[PacketProcessor.disableHandler]
     */
    fun disableHandler(opcode: Opcode) = packetProcessor.disableHandler(opcode)
}