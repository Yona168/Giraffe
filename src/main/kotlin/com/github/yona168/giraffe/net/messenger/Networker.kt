package com.github.yona168.giraffe.net.messenger

import com.github.yona168.giraffe.net.onDisable
import com.github.yona168.giraffe.net.onEnable
import com.github.yona168.giraffe.net.packet.Opcode
import com.github.yona168.giraffe.net.packet.PacketBuilder
import com.github.yona168.giraffe.net.packet.pool.ByteBufferWrapper
import com.github.yona168.giraffe.net.packet.pool.ReceivablePacketPool
import com.gitlab.avelyn.architecture.base.Component
import kotlinx.coroutines.*
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousChannel

typealias ReceivablePacket = ByteBufferWrapper


abstract class Networker : Component(), CoroutineScope, PacketHandler by PacketHandlerImpl(){
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

interface Writable{
   fun write(builder:PacketBuilder)
}