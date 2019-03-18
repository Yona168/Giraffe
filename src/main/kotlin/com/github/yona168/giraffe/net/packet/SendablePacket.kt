package com.github.yona168.giraffe.net.packet

import java.nio.ByteBuffer
import java.util.*

interface SendablePacket {
    fun writeInt(i: Int): SendablePacket
    fun writeByte(b: Byte): SendablePacket
    fun writeDouble(d: Double): SendablePacket
    fun writeLong(l: Long): SendablePacket
    fun writeShort(s: Short): SendablePacket
    fun writeBytes(vararg b: Byte): SendablePacket
    fun writeString(s: String): SendablePacket

    @JvmDefault
    fun writeUUID(uuid: UUID) = this.apply {
        writeLong(uuid.mostSignificantBits)
        writeLong(uuid.leastSignificantBits)
    }
    fun build(): ByteBuffer
}
