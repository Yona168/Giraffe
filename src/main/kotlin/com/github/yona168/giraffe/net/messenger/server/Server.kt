package com.github.yona168.giraffe.net.messenger.server

import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.packet.SendablePacket
import com.gitlab.avelyn.architecture.base.Toggleable
import java.util.*

interface Server : Toggleable {
    fun sendToClient(uuid: UUID, packet: SendablePacket): Boolean
    fun sendToAllClients(packetSupplier: () -> SendablePacket) = clients.forEach { it.write(packetSupplier()) }
    val clients: Collection<Writable>
}