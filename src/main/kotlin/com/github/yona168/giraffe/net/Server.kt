package com.github.yona168.giraffe.net

import com.github.yona168.giraffe.net.messenger.Networker
import com.github.yona168.giraffe.net.packet.PacketBuilder
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GiraffeServer(address: InetSocketAddress) : Networker() {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val channels = emptySet<Client>().toMutableSet()
    override lateinit var socketChannel: AsynchronousServerSocketChannel

    init {
        onEnable {
            socketChannel = AsynchronousServerSocketChannel.open()
            socketChannel.bind(address)
            launch {
                while (true) {
                    val channel = accept()
                    println("Channel Accepted!")
                    channels.add(Client(channel))
                }
            }
        }
    }

    private suspend fun accept(): AsynchronousSocketChannel =
        suspendCancellableCoroutine { continuation ->
            socketChannel.accept(continuation, AcceptHandler)
        }

    fun sendToClient(client: Client, packet: PacketBuilder) = launch {
        client.write(packet)
    }
    fun sendToAllClients(packet:PacketBuilder)=channels.forEach{sendToClient(it, packet)}

}

object AcceptHandler : ContinuationCompletionHandler<AsynchronousSocketChannel>



