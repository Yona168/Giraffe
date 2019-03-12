package com.github.yona168.giraffe.net.packet

import com.github.yona168.giraffe.net.Opcode
import java.util.function.Consumer


inline fun packetBuilder(opcode: Opcode, packetFunc: SendablePacket.() -> Unit): SendablePacket {
    val packet = QueuedOpPacket(opcode)
    packetFunc(packet)
    return packet
}

fun packetBuilder(opcode: Opcode, packetFunc: Consumer<SendablePacket>): SendablePacket {
    val packet=QueuedOpPacket(opcode)
    packetFunc.accept(packet)
    return packet
}