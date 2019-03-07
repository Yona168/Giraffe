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
                print("Packet Processor: ${packetProcessor.isEnabled}")
                this@AbstractScopedPacketChannelComponent.coroutineContext.cancelChildren()
                this@AbstractScopedPacketChannelComponent.coroutineContext.cancel()

                job.cancelChildren()
                println(job.isCancelled)
                println(this@AbstractScopedPacketChannelComponent.coroutineContext.isActive)
                try {
                    job.cancelAndJoin()
                } finally {
                    print(job.isCancelled)
                }
                initShutdown()
                print("Donezo")
            }
        }
    }

    protected abstract suspend fun initShutdown()
}


