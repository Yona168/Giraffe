package com.github.yona168.giraffe.net.packet

import com.github.yona168.giraffe.net.Opcode
import java.util.*


fun uuidPacket(uuid: UUID) = packetBuilder(INTERNAL_OPCODE) {
    writeByte(HANDSHAKE_SUB_IDENTIFIER)
    writeUUID(uuid)
}


const val INTERNAL_OPCODE: Opcode = -1

//Client bound
const val HANDSHAKE_SUB_IDENTIFIER: Opcode = 0
