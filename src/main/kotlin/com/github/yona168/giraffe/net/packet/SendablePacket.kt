package com.github.yona168.giraffe.net.packet

import java.nio.ByteBuffer
import java.util.*

interface SendablePacket {
    fun writeInt(i: Int)
    fun writeByte(b: Byte)
    fun writeDouble(d: Double)
    fun writeLong(l: Long)
    fun writeShort(s: Short)
    fun writeBytes(vararg b: Byte)
    fun writeString(s: String)
    fun writeUUID(uuid: UUID) {
        writeLong(uuid.mostSignificantBits)
        writeLong(uuid.leastSignificantBits)
    }
    fun build(): ByteBuffer
}
