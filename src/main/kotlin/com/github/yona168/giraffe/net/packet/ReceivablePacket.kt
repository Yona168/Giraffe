package com.github.yona168.giraffe.net.packet

interface ReceivablePacket {
    fun readString(): String
    fun readInt(): Int
    fun readShort(): Short
    fun readByte(): Byte
    fun readDouble(): Double
    fun readFloat(): Float
    fun readLong(): Long
    fun readChar(): Char
    fun put(byte: Byte)
    fun flip()
    fun clear()
}