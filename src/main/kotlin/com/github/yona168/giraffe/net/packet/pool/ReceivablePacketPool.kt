package com.github.yona168.giraffe.net.packet.pool

import com.github.yona168.giraffe.net.MAX_PACKET_BYTE_SIZE
import com.github.yona168.giraffe.net.packet.ByteBufferWrapper
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import java.nio.ByteBuffer

class ReceivablePacketPool : AbstractPool<ReceivablePacket>() {
    override fun clear(item: ReceivablePacket)=item.also { item.clear() }
    override fun createNew(): ReceivablePacket =
        ByteBufferWrapper(ByteBuffer.allocate(MAX_PACKET_BYTE_SIZE))
}