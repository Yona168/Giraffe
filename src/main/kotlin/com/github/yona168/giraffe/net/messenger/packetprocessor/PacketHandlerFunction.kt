package com.github.yona168.giraffe.net.messenger.packetprocessor

import com.github.yona168.giraffe.net.messenger.client.Client
import com.github.yona168.giraffe.net.packet.ReceivablePacket

/**
 * A function that takes a processing client and a packet
 */
interface PacketHandlerFunction {
    fun handle(client: Client, packet: ReceivablePacket)
}