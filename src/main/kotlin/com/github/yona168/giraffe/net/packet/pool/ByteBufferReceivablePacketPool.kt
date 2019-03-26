package com.github.yona168.giraffe.net.packet.pool

import com.github.yona168.giraffe.net.constants.MAX_PACKET_BYTE_SIZE
import com.github.yona168.giraffe.net.packet.ByteBufferReceivablePacket
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import java.nio.ByteBuffer

/**
 * A pool for [ByteBufferReceivablePacket]s.
 */
class ByteBufferReceivablePacketPool : QueuedPool<ReceivablePacket>() {
    /**
     * Clears the passed [item] by calling [ReceivablePacket.clear].
     * @return the cleared item.
     */
    override fun clear(item: ReceivablePacket) = item.apply(ReceivablePacket::clear)

    /**
     * @return a new [ByteBufferReceivablePacketPool] with a backed [ByteBuffer] of size [MAX_PACKET_BYTE_SIZE]
     */
    override fun createNew(): ByteBufferReceivablePacket =
        ByteBufferReceivablePacket(ByteBuffer.allocateDirect(MAX_PACKET_BYTE_SIZE))
}