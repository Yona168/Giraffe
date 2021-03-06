package com.github.yona168.giraffe.net.messenger.packetprocessor

import com.github.yona168.giraffe.net.constants.Opcode
import com.github.yona168.giraffe.net.messenger.client.Client
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import kotlinx.coroutines.launch

/**
 * A [PacketProcessor] that uses a [Map] to register/unregister [Opcode]s and packet handler functions from
 * each other.
 */
abstract class MappedPacketProcessor : PacketProcessorComponent() {

    private val handlerMap = mutableMapOf<Opcode, (Client, ReceivablePacket)->Unit>()

    override fun handle(client: Client, opcode: Opcode, packet: ReceivablePacket) = launch(coroutineContext) {
        handleByMap(opcode, packet, client)
    }


    private fun handleByMap(opcode: Opcode, packet: ReceivablePacket, networker: Client) {
        handlerMap[opcode]?.invoke(networker, packet)
    }

    override fun on(opcode: Opcode, func: (Client, ReceivablePacket)->Unit) = this.apply {
        handlerMap[opcode]=func
    }

    override fun disableHandler(opcode: Opcode) = this.apply {
        handlerMap.remove(opcode)
    }

}