package com.github.yona168.giraffe.net.packet

import com.github.yona168.giraffe.net.Opcode
import java.util.*


fun uuidPacket(uuid: UUID) = packetBuilder(INTERNAL_OPCODE) {
    writeByte(HANDSHAKE_SUB_IDENTIFIER)
    writeLong(uuid.mostSignificantBits)
    writeLong(uuid.leastSignificantBits)
}

fun disconnectRequest(uuidOfClient: UUID) = packetBuilder(INTERNAL_OPCODE) {
    writeByte(DISCONNECT_REQUEST_SUB_IDENTIFIER)
    writeLong(uuidOfClient.mostSignificantBits)
    writeLong(uuidOfClient.leastSignificantBits)
}

fun disconnectConfirmation() = packetBuilder(INTERNAL_OPCODE) {
    writeByte(DISCONNECT_CONFIRMATION_SUB_IDENTIFIER)
}

const val INTERNAL_OPCODE: Opcode = -1
const val HANDSHAKE_SUB_IDENTIFIER: Opcode = 0
const val DISCONNECT_REQUEST_SUB_IDENTIFIER: Opcode = 1
const val DISCONNECT_CONFIRMATION_SUB_IDENTIFIER: Opcode = 2