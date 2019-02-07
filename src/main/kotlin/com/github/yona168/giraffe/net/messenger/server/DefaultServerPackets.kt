package com.github.yona168.giraffe.net.messenger.server

import com.github.yona168.giraffe.net.packet.packetBuilder
import java.util.*


fun uuidPacket(uuid: UUID) = packetBuilder(SESSION_UUID_PACKET_OPCODE) {
    writeLong(uuid.mostSignificantBits)
    writeLong(uuid.leastSignificantBits)
}

const val SESSION_UUID_PACKET_OPCODE: Short = 0