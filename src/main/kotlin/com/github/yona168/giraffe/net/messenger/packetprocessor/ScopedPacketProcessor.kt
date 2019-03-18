package com.github.yona168.giraffe.net.messenger.packetprocessor

import com.github.yona168.giraffe.net.Opcode
import com.github.yona168.giraffe.net.PacketHandlerFunction
import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.onDisable
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import com.gitlab.avelyn.architecture.base.Component
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.withContext


abstract class ScopedPacketProcessor : PacketProcessor, Component() {
    val job = Job()

    init {
        onDisable {
            this.coroutineContext.cancel()
        }
    }

    private val handlerMap = mutableMapOf<Opcode, PacketHandlerFunction>()

    override suspend fun handle(opcode: Opcode, packet: ReceivablePacket, networker: Writable) {
        withContext(coroutineContext) {
            handleByMap(opcode, packet, networker)
        }
    }

    private fun handleByMap(opcode: Opcode, packet: ReceivablePacket, networker: Writable) {
        handlerMap[opcode]?.invoke(packet, networker)
    }

    override fun on(opcode: Opcode, func: PacketHandlerFunction): PacketHandlerFunction? {
        return handlerMap.put(opcode, func)
    }

    override fun disableHandler(opcode: Opcode): PacketHandlerFunction? {
        return handlerMap.remove(opcode)
    }

}