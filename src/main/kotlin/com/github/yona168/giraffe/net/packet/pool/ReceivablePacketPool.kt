package com.github.yona168.giraffe.net.packet.pool

import com.github.yona168.giraffe.net.maxByteLength
import com.github.yona168.giraffe.net.messenger.ReceivablePacket
import java.nio.ByteBuffer
import java.util.*

class ReceivablePacketPool : Pool<ReceivablePacket> {
    val buffer: ReceivablePacket
        get() = super.get()
    override val queue: Queue<ReceivablePacket> = ArrayDeque<ReceivablePacket>()
    override fun createNew(): ReceivablePacket = ByteBufferWrapper(ByteBuffer.allocate(maxByteLength))
}