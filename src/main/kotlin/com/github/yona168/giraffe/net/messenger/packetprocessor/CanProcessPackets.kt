package com.github.yona168.giraffe.net.messenger.packetprocessor

import com.github.yona168.giraffe.net.Opcode
import com.github.yona168.giraffe.net.PacketHandlerFunction
import com.github.yona168.giraffe.net.messenger.client.IClient
import com.github.yona168.giraffe.net.packet.ReceivablePacket

interface CanProcessPackets {
    val packetProcessor: PacketProcessor
    suspend fun handle(opcode: Opcode, packet: ReceivablePacket, networker: IClient) =
        packetProcessor.handle(opcode, packet, networker)

    fun on(opcode: Opcode, func: PacketHandlerFunction) = packetProcessor.on(opcode, func)
    fun disableHandler(opcode: Opcode) = packetProcessor.disableHandler(opcode)
}