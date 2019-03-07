package com.github.yona168.giraffe.net.messenger.packetprocessor

import com.github.yona168.giraffe.net.Opcode
import com.github.yona168.giraffe.net.PacketHandlerFunction
import com.github.yona168.giraffe.net.messenger.Writable
import com.github.yona168.giraffe.net.onDisable
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import com.gitlab.avelyn.architecture.base.Component
import kotlinx.coroutines.*

@ExperimentalCoroutinesApi
abstract class ScopedPacketProcessor : PacketProcessor, CoroutineScope, Component() {
    val job = Job()

    init {
        onDisable {
            cancel()
        }
    }

    private val handlerMap = mutableMapOf<Opcode, PacketHandlerFunction>()

    override suspend fun handlePacket(opcode: Opcode, packet: ReceivablePacket, networker: Writable) {
        withContext(coroutineContext) {
            handleByMap(opcode, packet, networker)
        }
    }

    private fun handleByMap(opcode: Opcode, packet: ReceivablePacket, networker: Writable) {
        handlerMap[opcode]?.invoke(packet, networker)
    }

    override fun registerHandler(opcode: Opcode, func: PacketHandlerFunction) {
        handlerMap[opcode] = func
    }

    override fun disableHandler(opcode: Opcode) {
        handlerMap.remove(opcode)
    }

}