package com.github.yona168.giraffe.net.messenger

import com.github.yona168.giraffe.net.connect.SuspendCloseable
import com.github.yona168.giraffe.net.messenger.packetprocessor.CanProcessPackets
import com.github.yona168.giraffe.net.messenger.packetprocessor.PacketProcessor
import com.github.yona168.giraffe.net.onDisable
import com.github.yona168.giraffe.net.packet.ReceivablePacket
import com.github.yona168.giraffe.net.packet.pool.Pool
import com.github.yona168.giraffe.net.packet.pool.ReceivablePacketPool
import com.gitlab.avelyn.architecture.base.Component
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import java.nio.channels.AsynchronousChannel

/**
 * This class links a [Component]'s enable/disable features to the jobs launched from
 * a [CoroutineScope]. This is used a base class for [com.github.yona168.giraffe.net.messenger.server.GServer] and
 * [com.github.yona168.giraffe.net.messenger.client.GClient]
 */
abstract class AbstractScopedPacketChannelComponent @JvmOverloads constructor(
    override val packetProcessor: PacketProcessor,
    protected val bufferPool: Pool<ReceivablePacket> = ReceivablePacketPool()
) : Component(),
    CanProcessPackets, SuspendCloseable, CoroutineScope {

    abstract val socketChannel: AsynchronousChannel
    val job = Job()

    init {
        onDisable {
            runBlocking {
                close()
            }
        }
    }

    protected fun cancelCoroutines() {
        this.coroutineContext.cancel()
    }


}


