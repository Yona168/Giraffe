package com.github.yona168.giraffe.net.messenger

import com.github.yona168.giraffe.net.messenger.packetprocessor.CanProcessPackets
import com.github.yona168.giraffe.net.messenger.packetprocessor.PacketProcessor
import com.github.yona168.giraffe.net.onDisable
import com.github.yona168.giraffe.net.onEnable
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import com.github.yona168.giraffe.net.packet.pool.ByteBufferReceivablePacketPool
import com.github.yona168.giraffe.net.packet.pool.Pool
import com.gitlab.avelyn.architecture.base.Component
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.nio.channels.AsynchronousChannel

/**
 * Provides base [Component] functionality for objects with [AsynchronousChannel]s and a [PacketProcessor].
 * On enable, the [packetProcessor] is enabled, and on disable [initClose] is first ran, [coroutineContext] is canelled, and finally,
 * [socketChannel] is closed.
 *
 * @param[packetProcessor] the [PacketProcessor] of this object
 * @param[bufferPool] the [Pool] of [ReceivablePacket]s to use to process packets with.
 */
abstract class AbstractScopedPacketChannelComponent @JvmOverloads constructor(
    override val packetProcessor: PacketProcessor,
    protected val bufferPool: Pool<ReceivablePacket> = ByteBufferReceivablePacketPool()
) : Component(),
    CanProcessPackets, CoroutineScope {
    abstract val socketChannel: AsynchronousChannel
    val job = Job()

    init {
        onEnable {
            packetProcessor.enable()
        }
        onDisable {
            runBlocking {
                initClose()
                this@AbstractScopedPacketChannelComponent.coroutineContext.cancel()
                if (socketChannel.isOpen) {
                    socketChannel.close()
                }
            }
        }
    }

    /**
     * Shutdown processes to happen before [socketChannel] is closed and [coroutineContext] is cancelled.
     */
    protected abstract suspend fun initClose()

}


