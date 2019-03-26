package com.github.yona168.giraffe.net.packet

import java.nio.ByteBuffer

/**
 * A [ReceivablePacket] backed by a [ByteBuffer].
 * @param[buffer] the buffer to back this packet (should be in default writing mode!)
 */
class ByteBufferReceivablePacket(private val buffer: ByteBuffer) : ReceivablePacket {
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

    override fun put(byte: Byte) {
        buffer.put(byte)
    }

    override fun prepareRead() {
        buffer.flip()
    }

    override fun clear() {
        buffer.clear()
    }
}

