package com.github.yona168.giraffe.net.messenger

import com.github.yona168.giraffe.net.messenger.packetprocessor.CanProcessPackets
import com.github.yona168.giraffe.net.messenger.packetprocessor.PacketProcessor
import com.github.yona168.giraffe.net.onDisable
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import com.github.yona168.giraffe.net.packet.pool.Pool
import com.github.yona168.giraffe.net.packet.pool.ReceivablePacketPool
import com.gitlab.avelyn.architecture.base.Component
import kotlinx.coroutines.*
import java.nio.channels.AsynchronousChannel


abstract class AbstractScopedPacketChannelComponent @JvmOverloads constructor(
    override val packetProcessor: PacketProcessor,
    protected val bufferPool: Pool<ReceivablePacket> = ReceivablePacketPool()

) : Component(),
    CanProcessPackets, CoroutineScope {

    protected abstract val socketChannel: AsynchronousChannel
    val job = Job()

    init {
        addChild(packetProcessor)
        onDisable {
            runBlocking {
                this.coroutineContext.cancelChildren()
                this.coroutineContext.cancel()
                job.cancelChildren()
                job.cancelAndJoin()
                socketChannel.close()
            }
        }
    }
}


