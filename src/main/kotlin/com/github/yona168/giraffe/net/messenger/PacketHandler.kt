package com.github.yona168.giraffe.net.messenger

import com.github.yona168.giraffe.net.Opcode
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import kotlinx.coroutines.*
import kotlinx.coroutines.NonCancellable.cancel
import kotlin.coroutines.CoroutineContext

typealias PacketHandlerFunction = (ReceivablePacket, Writable) -> Unit

interface PacketHandler {
    fun registerHandler(opcode: Opcode, func: PacketHandlerFunction)
    fun disablePacketHandler(opcode: Opcode)
    fun handlePacket(opcode: Opcode, packet: ReceivablePacket, client: Writable)
    fun disable()
}

internal class PacketHandlerImpl : PacketHandler, CoroutineScope {
    override fun disable()=cancel()
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main
    private val handlerMap = mutableMapOf<Opcode, PacketHandlerFunction>()
    override fun registerHandler(opcode: Opcode, func: PacketHandlerFunction) {
        handlerMap[opcode] = func
    }

    override fun handlePacket(opcode: Opcode, packet: ReceivablePacket, client: Writable) {
        this.launch {
            handlerMap[opcode]?.invoke(packet, client)
        }
    }

    override fun disablePacketHandler(opcode: Opcode) {
        handlerMap.remove(opcode)
    }

}