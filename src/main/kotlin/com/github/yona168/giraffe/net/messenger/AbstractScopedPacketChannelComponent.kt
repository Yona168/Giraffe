package com.github.yona168.giraffe.net.messenger

import com.github.yona168.giraffe.net.messenger.packetprocessor.CanProcessPackets
import com.github.yona168.giraffe.net.messenger.packetprocessor.ScopedPacketProcessor
import com.github.yona168.giraffe.net.onDisable
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import com.github.yona168.giraffe.net.packet.pool.Pool
import com.github.yona168.giraffe.net.packet.pool.ReceivablePacketPool
import com.gitlab.avelyn.architecture.base.Component
import kotlinx.coroutines.*
import java.nio.channels.AsynchronousChannel

/**
 * This class links a [Component]'s enable/disable features to the jobs launched from
 * a [CoroutineScope]. This is used a base class for [com.github.yona168.giraffe.net.messenger.server.GServer] and
 * [com.github.yona168.giraffe.net.messenger.client.GClient]
 */
@ExperimentalCoroutinesApi
abstract class AbstractScopedPacketChannelComponent @JvmOverloads constructor(
    override val packetProcessor: ScopedPacketProcessor,
    protected val bufferPool: Pool<ReceivablePacket> = ReceivablePacketPool()

) : Component(),
    CanProcessPackets, CoroutineScope {

    protected abstract val socketChannel: AsynchronousChannel
    val job = Job()

    init {
        addChild(packetProcessor)
        onDisable {
            runBlocking {
                packetProcessor.coroutineContext.cancelChildren()
                packetProcessor.coroutineContext.cancel()
                this@AbstractScopedPacketChannelComponent.coroutineContext.cancelChildren()
                this@AbstractScopedPacketChannelComponent.coroutineContext.cancel()

                job.cancelChildren()
                try {
                    job.cancelAndJoin()
                } finally {
                }
                initShutdown()
                print("Donezo")
            }
        }
    }

    protected abstract suspend fun initShutdown()
}


