package com.github.yona168.giraffe.net.messenger.server

import com.github.yona168.giraffe.net.connect.SuspendCloseable
import com.github.yona168.giraffe.net.messenger.client.IClient
import com.github.yona168.giraffe.net.messenger.packetprocessor.CanProcessPackets
import com.github.yona168.giraffe.net.packet.SendablePacket
import com.gitlab.avelyn.architecture.base.Toggleable
import kotlinx.coroutines.CoroutineScope
import java.util.*

interface IServer : Toggleable, SuspendCloseable, CanProcessPackets, CoroutineScope {

    val clients: Collection<IClient>

    fun closeClient(uuid: UUID)

    fun sendToClient(uuid: UUID, packet: SendablePacket): Boolean

    @JvmDefault
    fun sendToAllClients(packetSupplier: () -> SendablePacket) = clients.forEach { it.write(packetSupplier()) }

}