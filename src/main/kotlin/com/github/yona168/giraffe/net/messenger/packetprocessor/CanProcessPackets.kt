package com.github.yona168.giraffe.net.messenger.packetprocessor

import com.github.yona168.giraffe.net.constants.Opcode
import com.github.yona168.giraffe.net.messenger.client.Client
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import kotlinx.coroutines.Job
import java.util.function.BiConsumer

/**
 * An object that has a [PacketProcessor] as a field
 * @property[packetProcessor] The [PacketProcessor] contained by the implementing class
 * @see[PacketProcessor]
 */
interface CanProcessPackets {
    val packetProcessor: PacketProcessor
    /**
     * Calls [PacketProcessor.handle] with [packetProcessor]
     * @see[PacketProcessor.handle]
     * @return this for chaining
     */
    @JvmDefault
     fun handle(client: Client, opcode: Opcode, packet: ReceivablePacket)=packetProcessor.handle(client, opcode, packet)

    /**
     * Calls [PacketProcessor.on] with [packetProcessor]
     * @see[PacketProcessor.on]
     * @return this for chaining
     */
    @JvmDefault
    fun on(opcode: Opcode, func: (Client, ReceivablePacket)->Unit): CanProcessPackets {
        packetProcessor.on(opcode, func)
        return this
    }
    @JvmDefault
    fun on(opcode: Opcode, func: BiConsumer<Client, ReceivablePacket>)=on(opcode){ client, packet->func.accept(client, packet)}

    /**
     * Calls [PacketProcessor.disableHandler] with [packetProcessor]
     * @see[PacketProcessor.disableHandler]
     * @return this for chaining
     */
    @JvmDefault
    fun disableHandler(opcode: Opcode): CanProcessPackets {
        packetProcessor.disableHandler(opcode)
        return this
    }
}