package com.github.yona168.giraffe.net.packet

import com.github.yona168.giraffe.net.ByteBufferOp
import com.github.yona168.giraffe.net.MAX_PACKET_BYTE_SIZE
import com.github.yona168.giraffe.net.Opcode
import com.github.yona168.giraffe.net.Size
import java.nio.ByteBuffer
import java.util.*

class QueuedOpPacket constructor(private val opcode: Opcode) : SendablePacket {

    private val queueOperations = ArrayDeque<ByteBufferOp>()
    private var amtBytes: Size = 0
    private var firstBuild = false
    override fun writeInt(i: Int) = write(Int.SIZE_BYTES) { putInt(i) }
    override fun writeByte(b: Byte) = write(Byte.SIZE_BYTES) { put(b) }
    override fun writeDouble(d: Double) = write(java.lang.Double.BYTES) { putDouble(d) }
    override fun writeLong(l: Long) = write(Long.SIZE_BYTES) { putLong(l) }
    override fun writeShort(s: Short) = write(Short.SIZE_BYTES) { putShort(s) }
    override fun writeBytes(vararg b: Byte) = write(Byte.SIZE_BYTES * b.size) { put(b) }
    override fun writeString(s: String) = this.apply {
        val bytes = s.toByteArray()
        writeInt(bytes.size)
        writeBytes(*bytes)
    }

    private fun write(size: Int, op: ByteBufferOp) = this.apply {
        if (amtBytes + size > MAX_PACKET_BYTE_SIZE) {
            throw IllegalArgumentException("Packet length cannot exceed $MAX_PACKET_BYTE_SIZE. Your size would have been ${MAX_PACKET_BYTE_SIZE + amtBytes}")
        } else {
            amtBytes += size
            queueOperations.offerLast(op)
        }
    }

    override fun build(): ByteBuffer {
        val buffer = ByteBuffer.allocate(amtBytes + Size.SIZE_BYTES + Opcode.SIZE_BYTES)
        if (!firstBuild) {
            firstBuild = true
            queueOperations.offerFirst { putInt(amtBytes) }
            queueOperations.offerFirst { put(opcode) }
        }
        queueOperations.forEach { it(buffer) }
        buffer.flip()
        return buffer
    }
}

