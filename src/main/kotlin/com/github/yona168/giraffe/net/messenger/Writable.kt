package com.github.yona168.giraffe.net.messenger

import com.github.yona168.giraffe.net.packet.SendablePacket

interface Writable {
    fun write(packet: SendablePacket)
}