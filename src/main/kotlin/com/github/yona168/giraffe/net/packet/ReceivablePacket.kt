package com.github.yona168.giraffe.net.packet

import java.util.*

interface ReceivablePacket {
    fun readString(): String
    fun readInt(): Int
    fun readShort(): Short
    fun readByte(): Byte
    fun readDouble(): Double
    fun readFloat(): Float
    fun readLong(): Long
    fun readChar(): Char
    @JvmDefault
    fun readUUID(): UUID {
        return UUID(readLong(), readLong())
    }

    fun put(byte: Byte)
    fun flip()
    fun clear()
}