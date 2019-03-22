package com.github.yona168.giraffe.net.packet

import com.github.yona168.giraffe.net.Opcode
import java.util.function.Consumer

fun packetBuilder(opcode: Opcode, packetFunc: Consumer<SendablePacket>): SendablePacket {
    val packet=QueuedOpPacket(opcode)
    packetFunc.accept(packet)
    return packet
}