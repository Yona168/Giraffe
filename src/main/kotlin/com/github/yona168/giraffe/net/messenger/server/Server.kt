package com.github.yona168.giraffe.net.messenger.server

import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.packet.PacketBuilder
import java.util.*

interface Server {
    fun sendToClient(writable:Writable, packet:PacketBuilder)
    fun sendToClient(uuid: UUID, packet:PacketBuilder):Boolean
    fun sendToAllClients(packet:PacketBuilder)=clients.forEach { sendToClient(it, packet) }
    val clients:Set<Writable>
}