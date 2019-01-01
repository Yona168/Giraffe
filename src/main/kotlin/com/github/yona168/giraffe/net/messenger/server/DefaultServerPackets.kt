package com.github.yona168.giraffe.net.messenger.server

import com.github.yona168.giraffe.net.packet.packet
import java.util.*

internal fun uuidPacket(uuid: UUID)=packet(0){
    writeLong(uuid.leastSignificantBits)
    writeLong(uuid.mostSignificantBits)
}