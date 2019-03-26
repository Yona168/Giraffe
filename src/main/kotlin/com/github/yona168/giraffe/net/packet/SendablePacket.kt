package com.github.yona168.giraffe.net.packet

import com.github.yona168.giraffe.net.messenger.client.Client
import java.nio.ByteBuffer
import java.util.*

/**
 * A packet that can be sent to a [Client] with [Client.write].
 */
interface SendablePacket {
    /**
     * Writes an [Int] into this packet.
     * @param[i] the [Int] to write.
     * @return this for chaining.
     */
    fun writeInt(i: Int): SendablePacket

    /**
     * Writes a [Byte] into this packet.
     * @param[b] the [Byte] to write.
     * @return this for chaining.
     */
    fun writeByte(b: Byte): SendablePacket

    /**
     * Writes a [Double] into this packet.
     * @param[d] the [Double] to write
     * @return this for chaining.
     */
    fun writeDouble(d: Double): SendablePacket

    /**
     * Writes a [Long] into this packet.
     * @param[l] the [Long] to write.
     * @return this for chaining.
     */
    fun writeLong(l: Long): SendablePacket

    /**
     * Writes a [Short] into this packet.
     * @param[s] the [Short] to write.
     * @return this for chaining.
     */
    fun writeShort(s: Short): SendablePacket

    /**
     * Writes some [Byte]s into this packet.
     * @param[b] the [Byte]s to write.
     * @return this for chaining.
     */
    fun writeBytes(vararg b: Byte): SendablePacket

    /**
     * Writes a [String] into this packet.
     * @param[s] the [String] to write.
     * @return this for chaining.
     */
    fun writeString(s: String): SendablePacket

    /**
     * Writes a [UUID] into this packet by writing the UUID's [UUID.mostSigBits] and [UUID.leastSigBits] as [Long]s into this packet.
     * @return this for chaining.
     */
    @JvmDefault
    fun writeUUID(uuid: UUID) = this.apply {
        writeLong(uuid.mostSignificantBits)
        writeLong(uuid.leastSignificantBits)
    }

    /**
     * Puts all written objects into a [ByteBuffer]
     */
    fun build(): ByteBuffer
}
