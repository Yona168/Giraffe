package com.github.yona168.giraffe.net.messenger.packetprocessor

import com.github.yona168.giraffe.net.Opcode
import com.github.yona168.giraffe.net.PacketHandlerFunction
import com.github.yona168.giraffe.net.messenger.client.IClient
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import com.gitlab.avelyn.architecture.base.Toggleable
import kotlinx.coroutines.CoroutineScope

interface PacketProcessor : Toggleable, CoroutineScope {
    fun on(opcode: Opcode, func: PacketHandlerFunction): PacketHandlerFunction?
    fun disableHandler(opcode: Opcode): PacketHandlerFunction?
    suspend fun handle(opcode: Opcode, packet: ReceivablePacket, networker: IClient)

}