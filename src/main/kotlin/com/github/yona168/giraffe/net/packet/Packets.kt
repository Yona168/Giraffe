package com.github.yona168.giraffe.net.packet


inline fun packetBuilder(opcode: Short, packetFunc: SendablePacket.() -> Unit): SendablePacket {
    val packet = QueuedOpPacket(opcode)
    packetFunc(packet)
    return packet
}
