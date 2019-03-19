package com.github.yona168.giraffe.net.messenger

import com.github.yona168.giraffe.net.messenger.client.Client
import com.github.yona168.giraffe.net.messenger.packetprocessor.CanProcessPackets
import com.github.yona168.giraffe.net.messenger.packetprocessor.PacketProcessor
import com.github.yona168.giraffe.net.messenger.server.Server
import com.github.yona168.giraffe.net.onDisable
import com.github.yona168.giraffe.net.onEnable
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
 * This class links a [Component]'s enable/disableHandler features to the jobs launched from
 * a [CoroutineScope], and adds the pre enable/disable things useful for channel operations.
 * This is used a base class for [Server] and [Client]
 *
 */
abstract class AbstractScopedPacketChannelComponent @JvmOverloads constructor(
    override val packetProcessor: PacketProcessor,
    protected val bufferPool: Pool<ReceivablePacket> = ReceivablePacketPool()
) : Component(),
    CanProcessPackets, CoroutineScope {
    private val preEnable: MutableSet<Runnable> = mutableSetOf()
    private val preDisable: MutableSet<Runnable> = mutableSetOf()
    abstract val socketChannel: AsynchronousChannel
    val job = Job()

    init {
        onEnable {
            preEnable.forEach(Runnable::run)
            packetProcessor.enable()
        }
        onDisable {
            runBlocking {
                preDisable.forEach(Runnable::run)
                initClose()
                this@AbstractScopedPacketChannelComponent.coroutineContext.cancel()
                if (socketChannel.isOpen) {
                    socketChannel.close()
                }
            }
        }
    }

    protected abstract suspend fun initClose()

    fun preEnable(func: Runnable) = preEnable.add(func)

    fun preDisable(func: Runnable) = preDisable.add(func)

}


