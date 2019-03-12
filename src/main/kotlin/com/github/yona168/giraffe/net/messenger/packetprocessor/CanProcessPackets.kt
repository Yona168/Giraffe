package com.github.yona168.giraffe.net.messenger.packetprocessor

import com.github.yona168.giraffe.net.Opcode
import com.github.yona168.giraffe.net.PacketHandlerFunction
import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import java.util.function.BiConsumer

interface CanProcessPackets {
    val packetProcessor: PacketProcessor
    suspend fun handlePacket(opcode: Opcode, packet: ReceivablePacket, networker: Writable) =
        packetProcessor.handlePacket(opcode, packet, networker)

    fun register(opcode: Opcode, func: PacketHandlerFunction) = packetProcessor.reigster(opcode, func)
    fun register(opcode: Opcode, func: BiConsumer<ReceivablePacket, Writable>) =
        register(opcode) { packet, writable -> func.accept(packet, writable) }

    fun disableHandler(opcode: Opcode) = packetProcessor.disableHandler(opcode)
}