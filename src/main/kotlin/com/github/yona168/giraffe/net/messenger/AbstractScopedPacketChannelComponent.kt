package com.github.yona168.giraffe.net.messenger

import com.github.yona168.giraffe.net.onDisable
import com.github.yona168.giraffe.net.packet.pool.ReceivablePacketPool
import com.gitlab.avelyn.architecture.base.Component
import kotlinx.coroutines.*
import java.nio.channels.AsynchronousChannel


abstract class AbstractScopedPacketChannelComponent : Component(), CoroutineScope{

    protected abstract val socketChannel: AsynchronousChannel
    val bufferPool = ReceivablePacketPool()
    val job = Job()

    init {
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


