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
import java.nio.channels.AsynchronousChannel

/**
 * Provides base [Component] functionality for objects with [AsynchronousChannel]s and a [PacketProcessor].
 * Enable/Disable functionality is first handled by [ScopedComponent]. On enable, [packetProcessor] is enabled. On disable,
 * [initClose] is ran, and then [socketChannel] is closed
 *
 * @param[packetProcessor] the [PacketProcessor] of this object
 * @param[bufferPool] the [Pool] of [ReceivablePacket]s to use to process packets with.
 */
abstract class Messenger(
    override val packetProcessor: PacketProcessor,
    protected val bufferPool: Pool<ReceivablePacket> = ByteBufferReceivablePacketPool()
) : ScopedComponent(),
    CanProcessPackets {
    abstract val socketChannel: AsynchronousChannel
    init {
        onEnable {
            packetProcessor.enable()
        }
        onDisable {
            initClose()
            if (socketChannel.isOpen) {
                socketChannel.close()
            }
        }
    }

    /**
     * Shutdown processes to happen specific to the implementation. This happens after [CoroutineScope.cancel] is called.
     */
    protected abstract fun initClose()

}


