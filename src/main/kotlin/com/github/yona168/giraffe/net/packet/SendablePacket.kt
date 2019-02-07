package com.github.yona168.giraffe.net.packet

import java.nio.ByteBuffer

interface SendablePacket {
    fun writeInt(i: Int)
    fun writeByte(b: Byte)
    fun writeDouble(d: Double)
    fun writeLong(l: Long)
    fun writeShort(s: Short)
    fun writeBytes(vararg b: Byte)
    fun writeString(s: String)
    fun build(): ByteBuffer
}
