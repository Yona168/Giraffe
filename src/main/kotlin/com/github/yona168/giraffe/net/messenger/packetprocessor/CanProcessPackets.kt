package com.github.yona168.giraffe.net.messenger.packetprocessor

import com.github.yona168.giraffe.net.Opcode
import com.github.yona168.giraffe.net.PacketHandlerFunction
import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.packet.ReceivablePacket

interface CanProcessPackets {
    val packetProcessor: PacketProcessor
    suspend fun handlePacket(opcode: Opcode, packet: ReceivablePacket, networker: Writable) =
        packetProcessor.handlePacket(opcode, packet, networker)

    fun registerHandler(opcode: Opcode, func: PacketHandlerFunction) = packetProcessor.registerHandler(opcode, func)
    fun disableHandler(opcode: Opcode) = packetProcessor.disableHandler(opcode)
}