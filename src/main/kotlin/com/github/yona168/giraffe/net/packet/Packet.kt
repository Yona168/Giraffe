package com.github.yona168.giraffe.net.packet

import com.sun.jmx.remote.internal.ArrayQueue
import java.nio.ByteBuffer
import java.util.*

typealias ByteBufferOp = (ByteBuffer).() -> Unit

class Packet internal constructor() {
    private val queueOperations = ArrayDeque<ByteBufferOp>()
    private var amtBytes = 0
    val buffer: ByteBuffer by lazy {
        val buffer = ByteBuffer.allocate(amtBytes)
        queueOperations.forEach { it(buffer) }
        buffer
    }


    fun writeInt(i: Int) = write(Int.SIZE_BYTES) { putInt(i) }
    fun writeByte(b: Byte) = write(Byte.SIZE_BYTES) { put(b) }
    fun writeDouble(d: Double) = write(java.lang.Double.BYTES) { putDouble(d) }
    fun writeLong(l: Long) = write(Long.SIZE_BYTES) { putLong(l) }
    fun writeShort(s: Short) = write(Short.SIZE_BYTES) { putShort(s) }
    fun writeBytes(vararg b: Byte) = write(Byte.SIZE_BYTES * b.size) { put(b) }
    fun writeString(s: String) {
        val bytes = s.toByteArray()
        writeInt(bytes.size)
        writeBytes(*bytes)
    }

    private fun write(size: Int, op: ByteBufferOp) {
        amtBytes += size
        queueOperations.offerLast(op)
    }

}

fun packet(packetFunc: Packet.() -> Unit): Packet {
    val packet = Packet()
    packetFunc(packet)
    return packet
}