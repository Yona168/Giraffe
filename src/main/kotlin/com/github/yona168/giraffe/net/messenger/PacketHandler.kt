package com.github.yona168.giraffe.net.messenger

import com.github.yona168.giraffe.net.packet.Opcode

typealias PacketHandlerFunction = (ReceivablePacket) -> Unit
interface PacketHandler {
    fun registerHandler(opcode: Opcode, func:PacketHandlerFunction)
    fun handlePacket(opcode:Opcode, packet:ReceivablePacket)
}

internal class PacketHandlerImpl:PacketHandler {

    private val handlerMap= mutableMapOf<Opcode, PacketHandlerFunction>()
    override fun registerHandler(opcode: Opcode, func: PacketHandlerFunction) {
        handlerMap[opcode]=func
    }
    override fun handlePacket(opcode: Opcode, packet: ReceivablePacket) {
        handlerMap[opcode]?.invoke(packet)
    }

}