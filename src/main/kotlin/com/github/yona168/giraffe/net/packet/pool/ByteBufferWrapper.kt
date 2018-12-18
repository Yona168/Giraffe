package com.github.yona168.giraffe.net.packet.pool

import java.nio.ByteBuffer

class ByteBufferWrapper(internal val buffer: ByteBuffer) {
    fun readInt() = buffer.int
    fun readShort() = buffer.short
    fun readLong() = buffer.long
    fun readByte() = buffer.get()
    fun readChar() = buffer.char
    fun readDouble() = buffer.double
    fun readFloat() = buffer.float
    fun readString(): String {
        val size = readInt()
        val arr = ByteArray(size)
        buffer.get(arr, 0, size)
        return arr.contentToString()
    }

    internal fun put(b: Byte) = buffer.put(b)
    internal fun flip() = buffer.flip()
    internal fun clear() = buffer.clear()
}