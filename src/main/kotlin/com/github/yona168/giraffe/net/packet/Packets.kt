package com.github.yona168.giraffe.net.packet

import com.github.yona168.giraffe.net.constants.Opcode
import java.util.function.Consumer

/**
 * Function that creates a [QueuedOpSendablePacket] by creating it, passing it through the builder [packetFunc], and then returning it.
 * @param[opcode] the [Opcode] of the packet.
 * @param[packetFunc] the function to put the packet through.
 */
fun packetBuilder(opcode: Opcode, packetFunc: Consumer<SendablePacket>): QueuedOpSendablePacket {
    val packet = QueuedOpSendablePacket(opcode)
    packetFunc.accept(packet)
    return packet
}
