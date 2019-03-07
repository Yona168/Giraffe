package com.github.yona168.giraffe.net.packet

import java.util.function.Consumer


inline fun packetBuilder(opcode: Short, packetFunc: SendablePacket.() -> Unit): SendablePacket {
    val packet = QueuedOpPacket(opcode)
    packetFunc(packet)
    return packet
}

fun packetBuilder(opcode:Short, packetFunc: Consumer<SendablePacket>):SendablePacket{
    val packet=QueuedOpPacket(opcode)
    packetFunc.accept(packet)
    return packet
}