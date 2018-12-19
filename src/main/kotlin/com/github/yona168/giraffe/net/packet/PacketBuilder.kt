package com.github.yona168.giraffe.net.packet

import com.github.yona168.giraffe.net.maxByteLength
import java.nio.ByteBuffer
import java.util.*

typealias ByteBufferOp = (ByteBuffer).() -> Unit
typealias Opcode = Short
typealias Size = Int

class PacketBuilder internal constructor(val opcode: Opcode) {
    private val queueOperations = ArrayDeque<ByteBufferOp>()
    private var amtBytes: Size = 0
    val buffer: ByteBuffer by lazy {

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
        if (amtBytes + size > maxByteLength) {
            throw IllegalArgumentException("Packet length cannot exceed $maxByteLength. Your size would have been ${maxByteLength + amtBytes}")
        } else {
            amtBytes += size
            queueOperations.offerLast(op)
        }
    }

    internal fun build(): ByteBuffer {
        val buffer = ByteBuffer.allocate(amtBytes+Int.SIZE_BYTES+Opcode.SIZE_BYTES)
        queueOperations.offerFirst { putInt(amtBytes) }
        queueOperations.offerFirst { putShort(opcode) }
        queueOperations.forEach { it(buffer) }
        buffer.flip()
        return buffer
    }
}

fun packet(opcode: Short, packetFunc: PacketBuilder.() -> Unit): PacketBuilder {
    val packet = PacketBuilder(opcode)
    packetFunc(packet)
    return packet
}