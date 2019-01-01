package com.github.yona168.giraffe.net.messenger

import com.github.yona168.giraffe.net.messenger.client.Client
import com.github.yona168.giraffe.net.packet.Opcode
import com.github.yona168.giraffe.net.packet.ReceivablePacket

typealias PacketHandlerFunction = (ReceivablePacket, Writable) -> Unit

interface PacketHandler {
    fun registerHandler(opcode: Opcode, func: PacketHandlerFunction)
    fun disablePacketHandler(opcode:Opcode)
    fun handlePacket(opcode: Opcode, packet: ReceivablePacket, client:Writable)
}

internal class PacketHandlerImpl : PacketHandler {
    private val handlerMap = mutableMapOf<Opcode, PacketHandlerFunction>()
    override fun registerHandler(opcode: Opcode, func: PacketHandlerFunction) {
        handlerMap[opcode] = func
    }

    override fun handlePacket(opcode: Opcode, packet: ReceivablePacket,client:Writable) {
        handlerMap[opcode]?.invoke(packet, client)
    }
    override fun disablePacketHandler(opcode: Opcode){
        handlerMap.remove(opcode)
    }

}