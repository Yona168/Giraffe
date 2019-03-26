package com.github.yona168.giraffe.net.packet

import com.github.yona168.giraffe.net.constants.MAX_PACKET_BYTE_SIZE
import com.github.yona168.giraffe.net.constants.Opcode
import com.github.yona168.giraffe.net.constants.Size
import java.nio.ByteBuffer
import java.util.*
import java.util.function.Consumer

/**
 * An implementation of [SendablePacket] that queues writing operations until building. This allows for multiple builds
 * of a packet as well as easy ability to prepend information. This implementation builds a packet as follows:
 * 1. The opcode of the packet (of type [Opcode]).
 * 2. The size of the packet (of type [Size]).
 * 3. The rest of the bytes.
 *
 * @param[opcode] the Opcode of this packet.
 */
class QueuedOpSendablePacket(private val opcode: Opcode) : SendablePacket {
    /**
     * The queue to hold the operations
     */
    private val queueOperations = ArrayDeque<Consumer<ByteBuffer>>()
    private var amtBytes: Size = 0
    private var firstBuild = false
    override fun writeInt(i: Int) = write(Int.SIZE_BYTES, Consumer { buf -> buf.putInt(i) })
    override fun writeByte(b: Byte) = write(Byte.SIZE_BYTES, Consumer { buf -> buf.put(b) })
    override fun writeDouble(d: Double) = write(java.lang.Double.BYTES, Consumer { buf -> buf.putDouble(d) })
    override fun writeLong(l: Long) = write(Long.SIZE_BYTES, Consumer { buf -> buf.putLong(l) })
    override fun writeShort(s: Short) = write(Short.SIZE_BYTES, Consumer { buf -> buf.putShort(s) })
    override fun writeBytes(vararg b: Byte) = write(Byte.SIZE_BYTES * b.size, Consumer { buf -> buf.put(b) })
    override fun writeString(s: String) = this.apply {
        val bytes = s.toByteArray()
        writeInt(bytes.size)
        writeBytes(*bytes)
    }

    private fun write(size: Int, op: Consumer<ByteBuffer>) = this.apply {
        if (amtBytes + size > MAX_PACKET_BYTE_SIZE) {
            throw IllegalArgumentException("Packet length cannot exceed $MAX_PACKET_BYTE_SIZE. Your size would have been ${MAX_PACKET_BYTE_SIZE + amtBytes}")
        } else {
            amtBytes += size
            queueOperations.offerLast(op)
        }
    }

    /**
     * Builds the packet. This can be called multiple times because operations are queued.
     */
    override fun build(): ByteBuffer {
        val buffer = ByteBuffer.allocate(amtBytes + Size.SIZE_BYTES + Opcode.SIZE_BYTES)
        if (!firstBuild) {
            firstBuild = true
            queueOperations.offerFirst(Consumer { buf -> buf.putInt(amtBytes) })
            queueOperations.offerFirst(Consumer { buf -> buf.put(opcode) })
        }
        queueOperations.forEach { it.accept(buffer) }
        buffer.flip()
        return buffer
    }
}

