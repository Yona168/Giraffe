package com.github.yona168.giraffe.net.messenger.server

import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.packet.Packet
import java.util.*

interface Server {
    fun sendToClient(writable:Writable, packet:Packet)
    fun sendToClient(uuid: UUID, packet:Packet):Boolean
    fun sendToAllClients(packet:Packet)=clients.forEach { sendToClient(it, packet) }
    val clients:Set<Writable>
}