package com.github.yona168.giraffe.net.packet

import java.nio.ByteBuffer

class ByteBufferWrapper(private val buffer: ByteBuffer) : ReceivablePacket {
    override fun readInt() = buffer.int
    override fun readShort() = buffer.short
    override fun readLong() = buffer.long
    override fun readByte() = buffer.get()
    override fun readChar() = buffer.char
    override fun readDouble() = buffer.double
    override fun readFloat() = buffer.float
    override fun readString(): String {
        val size = readInt()
        val arr = ByteArray(size)
        buffer.get(arr, 0, size)
        return arr.map { it.toChar() }.joinToString("")
    }

    override fun put(b: Byte) {
        buffer.put(b)
    }

    override fun flip() {
        buffer.flip()
    }

    override fun clear() {
        buffer.clear()
    }
}

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