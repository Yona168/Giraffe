package com.github.yona168.giraffe.net.packet

import com.github.yona168.giraffe.net.messenger.packetprocessor.PacketProcessor
import java.nio.ByteBuffer
import java.util.*

/**
 * A packet that can be processed by a [PacketProcessor].
 */
interface ReceivablePacket {
    /**
     * Reads a [String] from this packet.
     * @return this for chaining.
     */
    fun readString(): String

    /**
     * Reads an [Int] from this packet.
     * @return this for chaining.
     */
    fun readInt(): Int

    /**
     * Reads a [Short] from this packet.
     * @return this for chaining.
     */
    fun readShort(): Short

    /**
     * Reads a [Byte] from this packet.
     * @return this for chaining.
     */
    fun readByte(): Byte

    /**
     * Reads a [Double] from this packet.
     * @return this for chaining.
     */
    fun readDouble(): Double

    /**
     * Reads a [Float] from this packet.
     * @return this for chaining.
     */
    fun readFloat(): Float

    /**
     * Reads a [Long] from this packet.
     * @return this for chaining.
     */
    fun readLong(): Long

    /**
     * Reads a [Char] from this packet.
     * @return this for chaining.
     */
    fun readChar(): Char

    /**
     * Reads a [UUID] from this packet by reading its least and most significant bits as [Long]s.
     * @return this for chaining.
     */
    @JvmDefault
    fun readUUID(): UUID {
        return UUID(readLong(), readLong())
    }

    /**
     * Put a byte into this packet.
     * @param[byte] the [Byte] to put.
     */
    fun put(byte: Byte)

    /**
     * Prepares this packet for reading. ie a [ByteBuffer]-backed packet would call [ByteBuffer.flip]
     */
    fun prepareRead()

    /**
     * Clears this packet and prepares it for reuse. This allows usage of packet pooling to improve performance.
     */
    fun clear()
}