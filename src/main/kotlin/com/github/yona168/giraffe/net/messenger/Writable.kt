package com.github.yona168.giraffe.net.messenger

import com.github.yona168.giraffe.net.packet.PacketBuilder

interface Writable {
    fun write(packet:PacketBuilder)
}