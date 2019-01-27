package com.github.yona168.giraffe.net.messenger.packetprocessor

import com.github.yona168.giraffe.net.Opcode
import com.github.yona168.giraffe.net.PacketHandlerFunction
import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import com.gitlab.avelyn.architecture.base.Toggleable

interface PacketProcessor : Toggleable {
    fun registerHandler(opcode: Opcode, func: PacketHandlerFunction)
    fun disableHandler(opcode: Opcode)
    suspend fun handlePacket(opcode: Opcode, packet: ReceivablePacket, networker: Writable)

}