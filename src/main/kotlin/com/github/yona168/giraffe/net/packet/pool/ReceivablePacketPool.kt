package com.github.yona168.giraffe.net.packet.pool

import com.github.yona168.giraffe.net.maxByteLength
import com.github.yona168.giraffe.net.packet.ByteBufferWrapper
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import java.nio.ByteBuffer
import java.util.*

class ReceivablePacketPool : AbstractPool<ReceivablePacket>() {
    override fun clear(item: ReceivablePacket)=item.also { item.clear() }
    override fun createNew(): ReceivablePacket =
        ByteBufferWrapper(ByteBuffer.allocate(maxByteLength))
}