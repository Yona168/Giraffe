package com.github.yona168.giraffe.net.messenger

import com.github.yona168.giraffe.net.packet.Packet

interface Writable {
    fun write(packet:Packet)
}