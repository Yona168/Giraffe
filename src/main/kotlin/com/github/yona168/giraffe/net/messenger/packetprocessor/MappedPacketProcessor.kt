package com.github.yona168.giraffe.net.messenger.packetprocessor

import com.github.yona168.giraffe.net.constants.Opcode
import com.github.yona168.giraffe.net.messenger.client.Client
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import kotlinx.coroutines.withContext

/**
 * A [PacketProcessor] that uses a [Map] to register/unregister [Opcode]s and [PacketHandlerFunction]s from
 * each other.
 */
abstract class MappedPacketProcessor : PacketProcessorComponent() {

    private val handlerMap = mutableMapOf<Opcode, PacketHandlerFunction>()

    override suspend fun handle(opcode: Opcode, packet: ReceivablePacket, client: Client) {
        withContext(coroutineContext) {
            handleByMap(opcode, packet, client)
        }
    }

    private fun handleByMap(opcode: Opcode, packet: ReceivablePacket, networker: Client) {
        handlerMap[opcode]?.handle(networker, packet)
    }

    override fun on(opcode: Opcode, func: PacketHandlerFunction): PacketHandlerFunction? {
        return handlerMap.put(opcode, func)
    }

    override fun disableHandler(opcode: Opcode): PacketHandlerFunction? {
        return handlerMap.remove(opcode)
    }

}