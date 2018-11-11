package com.github.yona168.giraffe.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import java.net.InetSocketAddress
import java.nio.channels.AsynchronousServerSocketChannel
import java.nio.channels.AsynchronousSocketChannel
import java.nio.channels.CompletionHandler
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class GiraffeServer(address:InetSocketAddress) : Networker() {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job

    private val channels = emptySet<AsynchronousSocketChannel>().toMutableSet()
    private lateinit var socketChannel: AsynchronousServerSocketChannel

    init {
        onEnable {
            socketChannel= AsynchronousServerSocketChannel.open()
                socketChannel.bind(address)
                launch{accept()}
        }
    }

    private suspend fun accept(): AsynchronousSocketChannel =
        suspendCancellableCoroutine { continuation ->
            socketChannel.accept(Unit, object : CompletionHandler<AsynchronousSocketChannel, Unit> {
                override fun completed(result: AsynchronousSocketChannel, attachment: Unit?) {
                    continuation.resume(result)
                    channels.add(result)
                    print("Channel accepted")
                    launch { this@GiraffeServer.accept() }
                }

                override fun failed(exc: Throwable, attachment: Unit?) {
                    socketChannel.closeOnCancelOf(continuation)
                    continuation.resumeWithException(exc)
                }
            })
        }

}



